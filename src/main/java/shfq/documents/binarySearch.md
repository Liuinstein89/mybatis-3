二分查找针对的是一个有序的数组其思想还是比较简单的，我们假设这个有序数组的顺序是从小到大排序的，我们拿一个目标值与数组最中间的一个值进行比较，两个数进行比较有三种情况：

- 目标值等于中值
- 目标值小于中值
- 目标值大于中值

下面分别对这三种情况进行讨论：

- 目标值等于中值时直接返回中值索引
- 目标值小于中值时待查找的元素如果在数组中的话，那么它肯定是在中值的左侧
- 目标值大于中值时待查找的元素如果在数组中的话，那么它肯定是在中值的右侧

如果是第二种和第三种情况则不断地应用上面的规则，循环往复，查找的序列会不断地缩小，直到查找完整个数组为止。

二分查找的思想虽然简单，但在实现的过程中还有许多细节需要注意，一不留神就写错了。今天，写得时候还是写错了，后来仔细地思考了一下把易错的地方都整理了出来。

### 下面是递归实现：


    /**
     * 用左边界和右边界来确定一个序列，其中左边界是闭合的，右边界是也是闭合的，即[ ] 这种形式
     * @param integers 数组或列表
     * @param leftBoundary 待查找的序列的左边界
     * @param rightBoundary 待查找的序列的右边界
     * @param targetValue 待查找的元素的值
     * @return 返回待查找的元素的索引值，如果待查找的元素不在数组中的话则返回 -1
     */
    private static int binarySearch(List<Integer> integers, int leftBoundary, int rightBoundary, int targetValue) {
        if (rightBoundary - leftBoundary == -1) {
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
        int rightBoundary = integers.size()-1;

        while (rightBoundary-leftBoundary >= 0) {
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

下面用一段简单的代码来验证一下：

    public static void main(String[] args) {
        int a = 1500000000;
        int b = a;
        int c = a + b;
        System.out.println("a+b=" + c);
    }
    
输出结果：

    a+b=-1294967296
    
#### 退出条件的判断
我之前在写得时候由于退出条件判断错误，当待查找的元素值不在查找的数组内时会导致无限的递归调用，最终会导致栈溢出。

退出分为两种情况：

- 找到了待查找的元素，这种情况很简单，直接把对应的索引值返回即可。
- 待查找的元素不存在在

其中待查找的元素不存在，又分三种情况:

- 待查找的元素比数组中的第一个元素（最小的元素）还小，自然待查找的元素不在数组中
- 待查找的元素比数组中的最后一个元素（最大的元素）还大，自然待查找的元素不在数组中
- 待查找的元素比数组中相邻的两个元素中的一个还小，比另一个还大，这种情况下待查找的元素也不在数组中。比如，待查找的数组是[1, 3, 5]待查找的元素是 4 ，4 比相邻的[3, 5]两个元素中的 3 大，比相邻的两个元素中的 5 小，而相邻的[3, 5] 之间再没有任何元素了，所以 4 不在数组中，查找失败了。

所以查找失败的情况下应该有三种退出条件：

- 待查找的元素比数组中的第一个元素（最小的元素）还小，待查找的序列会不断地向左收缩，最后会只剩下第一个元素，此时 leftBoundary == 0 ，rightBoundary == 0 ，middleIndex == 0 ，由于待查找的元素比 middleIndex==0 对应的元素（第一个元素）还小，所以 rightBoundary == (middleIndex-1) 即 rightBoundary == -1 ，此时 leftBoundary == 0 。此时 rightBoundary < leftBoundary ，所以这两个边界确定的是一个空的序列，空的序列里肯定不可能包含要查找的值，所以查找失败，退出查找。
- 待查找的元素比数组中的最后一个元素（最大的元素）还大，待查找的序列会不断地向右收缩，最后会只剩下最后一个元素（最大的元素），此时 leftBoundary == (integers.size()-1)， rightBoundary == (integers.size()-1) ，middleIndex == (integers.size()-1) ，而目标值比 middleIndex == integers.size()-1 对应的值还要大，所以下次迭代时 leftBoundary == (middleIndex+1) ，即 leftBoundary == integers.size()-1+1 ，即 leftBoundary == integers.size() ，此时 rightBoundary == integers.size()-1 。rightBoundary < leftBoundary 所以这两个边界之间确定的是一个空的序列，空的序列里肯定不可能包含要查找的值，所以查找失败，退出查找。
- 待查找的元素比数组中相邻的两个元素中的一个还小，比另一个还大，这种情况下待查找的元素不在数组中。待查找的序列会不断地收缩到最后只剩两个元素，待查找的元素比其中的一个元素大，比另外一个元素小。此时 middleIndex == (leftBoundary + (rightBoundary-leftBoundary)/2)，即 middleIndex == (leftBoundary + 1/2)，即 middleIndex == leftBoundary，由于待查找的元素比 middleIndex 对应的值（即 leftBoundary 对应的值）要大，所以下次迭代时 leftBoundary == middleIndex + 1，此时 leftBoundary == rightBoundary 。再次迭代时 middleIndex == (leftBoundary + ((rightBoundary - leftBoundary)/2))，即 middleIndex == (leftBoundary + 0/2)，即 middleIndex == leftBoundary ，而 middleIndex 对应的值其实就是 leftBoundary 对应的值，也是 rightBoundary 对应的值（此时 middleIndex 、leftBoundary、rightBoundary 这三者的值是相等的）。所以待查找的值比 middleIndex 对应的值要小，所以下次迭代时 rightBoundary == middleIndex - 1 ，此时 middleIndex == leftBoundary ，即 rightBoundary == leftBoundary -1 。此时 rightBoundary < leftBoundary ，所以这两个边界确定的是一个空的序列，空的序列里肯定不可能包含要查找的值，所以查找失败，退出查找。

> 查找失败退出条件的总结：从上面的三种情况可以看出，查找不到元素时退出的条件是：(leftBoundary - rightBoundary) == -1 ，因为这两个边界确定的是一个空的序列，空的序列当中当然不可能含有我们要查找的任何值，当然 leftBoundary > rightBoundary 也是成立的。

#### 左边界和右边界的计算方法
我之前在计算左边界和右边界的时候犯了个错误，下面是我的计算方法（是错误的方法，大家引以为戒）：

- 计算左边界：当目标值大于中值时，leftBoundary = middleIndex;
- 计算右边界：当目标值小于中值时，rightBoundary = middleIndex; 
  
计算左、右边界的正确的方法：

- 计算左边界：当目标值大于中值时，leftBoundary = middleIndex + 1;
- 计算右边界：当目标值小于中值时，rightBoundary = middleIndex - 1;

为什么下边的方法是正确的而上边的却不对呢，这两种方法看起来挺像的，上面的这种方法在有时候也是有效的，有时候却不行。下面的这种方法是正确的，是一直有效的。这两种方法的区别在于，下边的方法由于每次迭代时会 +1/-1 ，所以待查找的序列会越来越小。如果待查找的元素在序列内的话会一定找到，如果不在序列内的话会发生越界，即 leftBoundary - rightBoundary == 1 满足退出条件。

而上边的这种方法不是每次都是收缩的，有时会陷入无限递归/死循环。下面我举个例子：
假如待查找的序列不断地缩小为（或者待查找的序列最初就是）[3, 4]，待查找的元素是 4 ，应用规则 middleIndex == leftBoundary + (rightBoundary - leftBoundary)/2 即 middleIndex == leftBoundary + 1/2 ，即 middleIndex == leftBoundary ，此时 middleIndex == leftBoundary ， rightBoundary == leftBoundary + 1 ，待查找的元素比 leftBoundary 对应的元素要大，所以下次迭代时 middleIndex == leftBoundary + (rightBoundary - leftBoundary)/2 ，即 middleIndex == leftBoundary + 1/2 即 middleIndex == leftBoundary 因为迭代前后 middleIndex 的值是没有发生变化的，迭代前 middleIndex 的值和迭代后的值都是 leftBoundary ，所以会造成无限递归/死循环。发生这样的原因是待查找的序列是两个相邻的元素，它们的中值索引始终等于左边的元素的索引，待查找的序列不会缩小，所以造成了无限递归/死循环。

上面的这个例子，本来待查找的序列应该向右收缩，但却没有收缩，下面我再举一个本来应该向左收缩，但却没有收缩的例子：
假如待查找的序列最初就是 [3, 4]，待查找的元素是 2 ，应用规则 middleIndex == leftBoundary + (rightBoundary - leftBoundary)/2 即 middleIndex == leftBoundary + 1/2 ，即 middleIndex == leftBoundary ，此时 middleIndex == leftBoundary ， rightBoundary == leftBoundary + 1 ，待查找的元素比 leftBoundary 对应的元素要小，所以下次迭代时 middleIndex == leftBoundary + (rightBoundary - leftBoundary)/2 ，即 middleIndex == leftBoundary + 1/2 即 middleIndex == leftBoundary 。因为迭代前后 middleIndex 的值是没有发生变化的，迭代前 middleIndex 的值和迭代后的值都是 leftBoundary ，所以会造成无限递归/死循环。发生这样的原因是待查找的序列是两个相邻的元素，它们的中值索引始终等于左边的元素的索引，待查找的序列不会缩小，所以造成了无限递归/死循环。
 
#### 调用二分查找方法时传递的参数值的选取
我之前在调用二分查找方法时在传递参数值的选取上犯了个错误，我的错误方法是：

    Integer[] integers = {3, 5, 7, 9, 11};
    List<Integer> integerList = Arrays.asList(integers);
    int index = binarySearch(integerList, 0, integerList.size(), 12);
    
这样会导致数组下标越界异常，为什么会发生呢？下面来分析一下：
     
最后数组不断地收缩，待查找的序列收缩为 [9, 11] 此时 leftBoundary == 4 ，rightBoundary == 5 ，middleIndex == 4 + (5-4)/2 ，即 middleIndex == 4 ，4 对应的值为 9 比待查找的元素 12 要小 ， 所以下次迭代时： leftBoundary == middleIndex + 1 ，即 leftBoundary == 5 ，此时 leftBoundary == 5 ，rightBoundary == 5 ，而 middleIndex == 5 + (5-5)/2 ，即 middleIndex == 5 ，此时会把 5 对应的值与 目标值 12 进行比较，而 5 已经下标越界了，数组的长度是 5 。
所以说，我这种选取右边界参数值是不对的。其实，如果非要这样做也是可以的，但是需要修改递归/循环的退出条件。所以，在传参的时候要注意参数值和退出条件是有关系的，是需要对应使用的，不能把两者杂糅使用。

下面是正确的（与上面的退出条件相对应的）选取右边界的方式：

    Integer[] integers = {3, 5, 7, 9, 11};
    List<Integer> integerList = Arrays.asList(integers);
    int index = binarySearch(integerList, 0, integerList.size()-1, 12);
    
 


        

    
    
    

        
        
          
        
        

