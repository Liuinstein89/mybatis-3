二分查找针对的是一个有序的数组其思想还是比较简单的，我们假设这个有序数组的顺序是从小到大排序的，我们拿一个目标值与数组最中间的一个值进行比较，两个数进行比较有三种情况：

- 目标值等于中值
- 目标值小于中值
- 目标值大于中值

下面分别对这三种情况进行讨论：

- 目标值等于中值时直接返回中值索引
- 目标值小于中值时待查找的元素如果在数组中的话，那么它肯定是在中值的左侧
- 目标值大于中值时带查找的元素如果在数组中的话，那么它肯定是在中值的右侧

如果是第二种和第三种情况则不断地应用上面的规则，循环往复，查找的序列在不断地缩小，直到查找完整个数组为止。

二分查找的思想虽然简单，但在实现的过程中还有许多细节需要注意，一不留神就写错了。今天，写得时候还是写错了，后来仔细地思考了一下把易错的地方都整理了出来。

### 下面是递归实现：


    /**
     * 用左边界和右边界来确定一个序列，其中左边界是闭合的，右边界是开的，即[ ) 这种形式
     * @param integers 数组或列表
     * @param leftBoundary 待查找的序列的左边界，其中左边界是闭合的
     * @param rightBoundary 待查找的序列的右边界，其中右边界是开的
     * @param targetValue 待查找的元素的值
     * @return 返回待查找的元素的索引值，如果待查找的元素不在数组中的话则返回 -1
     */
    private static int binarySearch(List<Integer> integers, int leftBoundary, int rightBoundary, int targetValue) {
        if (leftBoundary == integers.size() || rightBoundary == -1) {
            return -1;
        }
        int middleIndex = leftBoundary + (rightBoundary - leftBoundary) / 2;
        int middleValue = integers.get(middleIndex);
        if (targetValue == middleValue) {
            return middleIndex;
        } else if (targetValue < middleValue) {
            return binarySearch(integers, leftBoundary, middleIndex - 1, targetValue);
        } else {
            return binarySearch(integers, middleIndex + 1, rightBoundary, targetValue);
        }
    }
    
### 下面是相应的循环实现：
    
    /**
     * 循环二分查找
     * @param integers 待查找的数组或列表
     * @param targetValue 待查找的元素的值
     * @return 返回待查找的元素的索引值，如果待查找的元素不在数组中的话则返回 -1
     */
    private static int loopBinarySearch(List<Integer> integers, int targetValue) {
        if (integers == null) {
            return -1;
        }

        int leftBoundary = 0;
        int rightBoundary = integers.size();

        while (!(leftBoundary == integers.size() || rightBoundary == -1)) {
            int middleIndex = leftBoundary + (rightBoundary - leftBoundary) / 2;
            int middleValue = integers.get(middleIndex);
            if (targetValue == middleValue) {
                return middleIndex;
            } else if (targetValue < middleValue) {
                rightBoundary = middleIndex - 1;
            } else {
                leftBoundary = middleIndex + 1;
            }
        }

        return -1;
    }
### 需要注意的几点

#### 中值的选取
中值的选取有两种方法：
- int middleIndex = leftBoundary + (rightBoundary - leftBoundary) / 2;
- int middleIndex = (leftBoundary + rightBoundary) / 2;
第一种和第二种稍有不同，第一种可以避免整型值的上溢出。所有的数据类型在计算机中存储时都是有长度限制的，比如 int 类型在 java 中是占用 4 个字节的长度，所以 int 能表述的值是有范围限制的。
4 个字节的长度是 4*8=32 位，所以整型的表述范围是 [-2^31, 2^31-1]即[-2147483648, 2147483647]。
例如，leftBoundary = 1500000000，rightBoundary = 1500000000，从数学的角度来讲 leftBoundary + rightBoundary = 3000000000 是毫无疑问的，但在计算机中却不是这样的，因为 3000000000 > 2147483647，这个值已经超出了整型值的最大值，超出最大值之后，正数就变成了负数。
下面用一段简单的代码来测试一下：

    public static void main(String[] args) {
        int a = 1500000000;
        int b = a;
        int c = a + b;
        System.out.println("a+b=" + c);
    }
    
下面是输出结果：

    a+b=-1294967296
    
#### 退出条件的判断
我之前在写的时候由于退出条件判断错误，当待查找的元素值不在查找的数组内时会导致无限的递归调用，最终会导致栈溢出。
退出条件是：

    if (leftBoundary == integers.size() || rightBoundary == -1) {
        return -1;
    }
退出分为两种情况：

- 找到了待查找的元素，这种情况很简单，直接把对应的索引值返回即可。
- 待查找的元素不在
很明显这个退出条件是由两部分组成的：

- leftBoundary == integers.size()：
- rightBoundary == -1    
#### 左边界和右边界的计算方法
我之前在计算左边界和右边界的时候犯了个错误，下面是我的计算方法（是错误的方法，大家引以为戒）：

- 计算左边界：当目标值大于中值时，leftBoundary = middleIndex;
- 计算右边界：当目标值小于中值时，rightBoundary = middleIndex;   
        

    
    
    

        
        
        
        
        

