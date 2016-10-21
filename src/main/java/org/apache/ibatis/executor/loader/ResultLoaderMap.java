/**
 *    Copyright 2009-2015 the original author or authors.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package org.apache.ibatis.executor.loader;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.apache.ibatis.executor.BaseExecutor;
import org.apache.ibatis.executor.BatchResult;
import org.apache.ibatis.executor.ExecutorException;
import org.apache.ibatis.logging.Log;
import org.apache.ibatis.logging.LogFactory;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;

/**
 * @author Clinton Begin
 * @author Franta Mejta
 */
public class ResultLoaderMap {

  private final Map<String, LoadPair> loaderMap = new HashMap<String, LoadPair>();

  public void addLoader(String property, MetaObject metaResultObject, ResultLoader resultLoader) {
    String upperFirst = getUppercaseFirstProperty(property);
    if (!upperFirst.equalsIgnoreCase(property) && loaderMap.containsKey(upperFirst)) { // todo 不太明白
      throw new ExecutorException("Nested lazy loaded result property '" + property +
              "' for query id '" + resultLoader.mappedStatement.getId() +
              " already exists in the result map. The leftmost property of all lazy loaded properties must be unique within a result map.");
    }
    loaderMap.put(upperFirst, new LoadPair(property, metaResultObject, resultLoader));
  }

  public final Map<String, LoadPair> getProperties() {
    return new HashMap<String, LoadPair>(this.loaderMap);
  }

  public Set<String> getPropertyNames() {
    return loaderMap.keySet();
  }

  public int size() {
    return loaderMap.size();
  }

  public boolean hasLoader(String property) {
    return loaderMap.containsKey(property.toUpperCase(Locale.ENGLISH));
  }

  public boolean load(String property) throws SQLException {
    LoadPair pair = loaderMap.remove(property.toUpperCase(Locale.ENGLISH));
    if (pair != null) {
      pair.load();
      return true;
    }
    return false;
  }

  public void loadAll() throws SQLException {
    final Set<String> methodNameSet = loaderMap.keySet();
    String[] methodNames = methodNameSet.toArray(new String[methodNameSet.size()]);
    for (String methodName : methodNames) {
      load(methodName);
    }
  }

  private static String getUppercaseFirstProperty(String property) {
    String[] parts = property.split("\\."); // todo 属性里面可能有类似 a.b 这种属性？
    return parts[0].toUpperCase(Locale.ENGLISH);
  }

  /**
   * Property which was not loaded yet.
   * 封装了一个未加载的属性和加载这个属性所需要的各种条件
   */
  public static class LoadPair implements Serializable {

    private static final long serialVersionUID = 20130412;
    /**
     * Name of factory method which returns database connection.
     */
    private static final String FACTORY_METHOD = "getConfiguration";
    /**
     * Object to check whether we went through serialization..
     */
    private final transient Object serializationCheck = new Object();
    /**
     * Meta object which sets loaded properties. 元 result 对象，元 result 对象里有个原始对象，待加载的属性是这个原始对象的属性，这个原始对象是一个代理对象，通过 javassit 创建的对象，这个对象所属的类继承自某个 VO 。
     */
    private transient MetaObject metaResultObject;
    /**
     * Result loader which loads unread properties.
     */
    private transient ResultLoader resultLoader;
    /**
     * Wow, logger.
     */
    private transient Log log;
    /**
     * Factory class through which we get database connection.
     */
    private Class<?> configurationFactory;
    /**
     * Name of the unread property.
     */
    private String property;
    /**
     * ID of SQL statement which loads the property. sql 语句的 id ，通过这个 sql 语句可以加载出待加载的属性
     */
    private String mappedStatement;
    /**
     * Parameter of the sql statement. 查询 sql 语句所需要的参数
     */
    private Serializable mappedParameter;

    private LoadPair(final String property, MetaObject metaResultObject, ResultLoader resultLoader) {
      this.property = property;
      this.metaResultObject = metaResultObject;
      this.resultLoader = resultLoader;

      /* Save required information only if original object can be serialized. */ // todo 不清楚
      if (metaResultObject != null && metaResultObject.getOriginalObject() instanceof Serializable) {
        final Object mappedStatementParameter = resultLoader.parameterObject;

        /* @todo May the parameter be null? */
        if (mappedStatementParameter instanceof Serializable) {
          this.mappedStatement = resultLoader.mappedStatement.getId();
          this.mappedParameter = (Serializable) mappedStatementParameter;

          this.configurationFactory = resultLoader.configuration.getConfigurationFactory();
        } else {
          Log log = this.getLogger();
          if (log.isDebugEnabled()) {
            log.debug("Property [" + this.property + "] of ["
                    + metaResultObject.getOriginalObject().getClass() + "] cannot be loaded "
                    + "after deserialization. Make sure it's loaded before serializing "
                    + "forenamed object.");
          }
        }
      }
    }

    public void load() throws SQLException {
      /* These field should not be null unless the loadpair was serialized.
       * Yet in that case this method should not be called. */
      if (this.metaResultObject == null) {
        throw new IllegalArgumentException("metaResultObject is null");
      }
      if (this.resultLoader == null) {
        throw new IllegalArgumentException("resultLoader is null");
      }

      this.load(null);
    }

    public void load(final Object userObject) throws SQLException {
      System.out.println("userObject:" + userObject);
      if (this.metaResultObject == null || this.resultLoader == null) { // todo 这是什么情况
        if (this.mappedParameter == null) {
          throw new ExecutorException("Property [" + this.property + "] cannot be loaded because "
                  + "required parameter of mapped statement ["
                  + this.mappedStatement + "] is not serializable.");
        }
        System.out.println("test:1");
        final Configuration config = this.getConfiguration();
        final MappedStatement ms = config.getMappedStatement(this.mappedStatement);
        if (ms == null) {
          throw new ExecutorException("Cannot lazy load property [" + this.property
                  + "] of deserialized object [" + userObject.getClass()
                  + "] because configuration does not contain statement ["
                  + this.mappedStatement + "]");
        }

        this.metaResultObject = config.newMetaObject(userObject);
        this.resultLoader = new ResultLoader(config, new ClosedExecutor(), ms, this.mappedParameter,
                metaResultObject.getSetterType(this.property), null, null); // todo 为什么要传一个 ClosedExecutor ？？？
        System.out.println("test:2");
      }

      /* We are using a new executor because we may be (and likely are) on a new thread
       * and executors aren't thread safe. (Is this sufficient?)
       *
       * A better approach would be making executors thread safe. */
      if (this.serializationCheck == null) {
        final ResultLoader old = this.resultLoader;
        this.resultLoader = new ResultLoader(old.configuration, new ClosedExecutor(), old.mappedStatement,
                old.parameterObject, old.targetType, old.cacheKey, old.boundSql);
        System.out.println("test:3");
      }
      System.out.println("test:4");
      this.metaResultObject.setValue(property, this.resultLoader.loadResult());
    }

    private Configuration getConfiguration() {
      if (this.configurationFactory == null) {
        throw new ExecutorException("Cannot get Configuration as configuration factory was not set.");
      }

      Object configurationObject = null;
      try {
        final Method factoryMethod = this.configurationFactory.getDeclaredMethod(FACTORY_METHOD);
        if (!Modifier.isStatic(factoryMethod.getModifiers())) {
          throw new ExecutorException("Cannot get Configuration as factory method ["
                  + this.configurationFactory + "]#["
                  + FACTORY_METHOD + "] is not static.");
        }

        if (!factoryMethod.isAccessible()) {
          configurationObject = AccessController.doPrivileged(new PrivilegedExceptionAction<Object>() {
            @Override
            public Object run() throws Exception {
              try {
                factoryMethod.setAccessible(true);
                return factoryMethod.invoke(null);
              } finally {
                factoryMethod.setAccessible(false);
              }
            }
          });
        } else {
          configurationObject = factoryMethod.invoke(null);
        }
      } catch (final NoSuchMethodException ex) {
        throw new ExecutorException("Cannot get Configuration as factory class ["
                + this.configurationFactory + "] is missing factory method of name ["
                + FACTORY_METHOD + "].", ex);
      } catch (final PrivilegedActionException ex) {
        throw new ExecutorException("Cannot get Configuration as factory method ["
                + this.configurationFactory + "]#["
                + FACTORY_METHOD + "] threw an exception.", ex.getCause());
      } catch (final Exception ex) {
        throw new ExecutorException("Cannot get Configuration as factory method ["
                + this.configurationFactory + "]#["
                + FACTORY_METHOD + "] threw an exception.", ex);
      }

      if (!(configurationObject instanceof Configuration)) {
        throw new ExecutorException("Cannot get Configuration as factory method ["
                + this.configurationFactory + "]#["
                + FACTORY_METHOD + "] didn't return [" + Configuration.class + "] but ["
                + (configurationObject == null ? "null" : configurationObject.getClass()) + "].");
      }

      return Configuration.class.cast(configurationObject);
    }

    private Log getLogger() {
      if (this.log == null) {
        this.log = LogFactory.getLog(this.getClass());
      }
      return this.log;
    }
  }

  /**
   * todo 一种特殊的 executor 不支持更新、刷新语句 为什么不支持 doQuery()？？ 主要用于查询懒加载属性，所以不支持更新
   */
  private static final class ClosedExecutor extends BaseExecutor {

    public ClosedExecutor() {
      super(null, null);
    }

    @Override
    public boolean isClosed() {
      return true;
    }

    @Override
    protected int doUpdate(MappedStatement ms, Object parameter) throws SQLException {
      throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    protected List<BatchResult> doFlushStatements(boolean isRollback) throws SQLException {
      throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    protected <E> List<E> doQuery(MappedStatement ms, Object parameter, RowBounds rowBounds, ResultHandler resultHandler, BoundSql boundSql) throws SQLException {
      throw new UnsupportedOperationException("Not supported.");
    }
  }
}
