package shfq;

import java.util.Arrays;
import java.util.List;

/**
 * author:      shfq
 * description:
 * create date: 2016/6/3.
 */
public class Test1 {
    public static void main(String[] args) {
        Integer[] integers = {3, 5, 7, 9, 11};
        List<Integer> integerList = Arrays.asList(integers);
        System.out.println(binarySearch(integerList, 0, integerList.size(), 8));

    }

    //    private static int binarySearch(List<Integer> integers, int leftBoundary, int rightBoundary, int targetValue) {
//        if (rightBoundary - leftBoundary == 0 || rightBoundary - leftBoundary == -1) {
//            return -1;
//        }
//        int middleValue = integers.get((leftBoundary + rightBoundary) / 2);
//        if (targetValue == middleValue) {
//            return (leftBoundary + rightBoundary) / 2;
//        } else if (targetValue < middleValue) {
//            return binarySearch(integers, leftBoundary, (leftBoundary + rightBoundary) / 2-1, targetValue);
//        } else {
//            return binarySearch(integers, (leftBoundary + rightBoundary) / 2+1, rightBoundary, targetValue);
//        }
//    }

    /**
     * 递归二分查找
     * 用左边界和右边界来确定一个序列，其中左边界是闭合的，右边界是开的，即[ ) 这种形式
     * @param integers 待查找的数组或列表
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
//

//        private static int binarySearch(List<Integer> integers, int leftBoundary, int rightBoundary, int targetValue) {
//        if (leftBoundary == integers.size() || rightBoundary == -1) {
//            return -1;
//        }
//        int middleIndex = leftBoundary + (rightBoundary - leftBoundary) / 2;
//        int middleValue = integers.get(middleIndex);
//        if (targetValue == middleValue) {
//            return middleIndex;
//        } else if (targetValue < middleValue) {
//            return binarySearch(integers, leftBoundary, middleIndex - 1, targetValue);
//        } else {
//            return binarySearch(integers, middleIndex + 1, rightBoundary, targetValue);
//        }
//    }

    /**
     * 循环二分查找
     * @param integers 待查找的数组或列表
     * @param targetValue 待查找的元素的值
     * @return 返回待查找的元素的索引值，如果待查找的元素不在数组中的话则返回 -1
     */
//    private static int loopBinarySearch(List<Integer> integers, int targetValue) {
//        if (integers == null) {
//            return -1;
//        }
//
//        int leftBoundary = 0;
//        int rightBoundary = integers.size();
//
//        while (!(leftBoundary == integers.size() || rightBoundary == -1)) {
//            int middleIndex = leftBoundary + (rightBoundary - leftBoundary) / 2;
//            int middleValue = integers.get(middleIndex);
//            if (targetValue == middleValue) {
//                return middleIndex;
//            } else if (targetValue < middleValue) {
//                rightBoundary = middleIndex - 1;
//            } else {
//                leftBoundary = middleIndex + 1;
//            }
//        }
//
//        return -1;
//    }




}
