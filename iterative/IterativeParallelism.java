package info.kgeorgiy.ja.Laskin_Pavel.iterative;

import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;

public class IterativeParallelism implements info.kgeorgiy.java.advanced.iterative.ScalarIP {


    private <T> long parallelWork(boolean indexes, int threads, List<T> values, Comparator<? super T> comparator, boolean max, Predicate<? super T> predicate, boolean first, boolean sum) throws InterruptedException {
        if (values.isEmpty()) {
            return -1;
        }

        int len = values.size();
        int countOfThreads = Math.min(len, threads);
        Thread[] allThreads = new Thread[countOfThreads];
        int sizeThread = len / countOfThreads;
        int extraSize = len % countOfThreads;
        long[] answersInd = new long[countOfThreads];
        MyPair<T>[] answersMaxMin = new MyPair[countOfThreads];

        int prevEnd = 0;

        for (int i = 0; i < countOfThreads; i++) {
            int ind = i;
            int begin = prevEnd;
            int end;
            if (extraSize != 0) {
                end = begin + sizeThread + 1;
                extraSize -= 1;
            } else {
                end = begin + sizeThread;
            }
            prevEnd = end;
            allThreads[i] = new Thread(() -> {
                if (indexes) {
                    answersInd[ind] = testAllElements(begin, end, values, predicate, first, sum);
                } else {
                    answersMaxMin[ind] = compAllElements(begin, end, values, comparator, max);
                }
            });
            allThreads[i].start();
        }

        for (Thread thread : allThreads) {
            thread.join();
        }

        if (indexes) {
            return returnAnsInd(answersInd, first, sum);
        }
        return compReturnAns(answersMaxMin, comparator, max, countOfThreads);
    }

    private <T> int compReturnAns(MyPair<T>[] answers, Comparator<? super T> comparator, boolean max, int countOfThreads) {
        MyPair<T> answer = answers[0];
        for (int i = 1; i < countOfThreads; i++) {
            MyPair<T> cur = answers[i];
            int comp = comparator.compare(cur.value, answer.value);
            if (max ? comp > 0 : comp < 0) {
                answer = cur;
            }
        }
        return answer.index;
    }

    private <T> MyPair<T> compAllElements(int begin, int end, List<T> values, Comparator<? super T> comparator, boolean max) {
        MyPair<T> ans = new MyPair<>(begin, values.get(begin));
        MyPair<T> cur;
        for (int j = begin + 1; j < end; j++) {
            cur = new MyPair<>(j, values.get(j));
            int comp = comparator.compare(cur.value, ans.value);
            if (max ? comp > 0 : comp < 0) {
                ans = cur;
            }
        }
        return ans;
    }

    @Override
    public <T> int argMax(int threads, List<T> values, Comparator<? super T> comparator) throws InterruptedException {
        return (int) parallelWork(false, threads, values, comparator, true, null, false, false);
    }

    @Override
    public <T> int argMin(int threads, List<T> values, Comparator<? super T> comparator) throws InterruptedException {
        return (int) parallelWork(false, threads, values, comparator, false, null, false, false);
    }


    private long returnAnsInd(long[] answers, boolean first, boolean sum) {
        long answerLast = -1;
        long answerFirst = -1;
        long answerSum = 0;
        for (long el : answers) {
            if (el != -1) {
                answerLast = el;
                answerSum += el + 1;
                if (first && answerFirst == -1) {
                    answerFirst = el;
                }
            }
        }
        return sum ? answerSum : (first ? answerFirst : answerLast);
    }

    private <T> long testAllElements(int begin, int end, List<T> values, Predicate<? super T> predicate, boolean first, boolean sum) {
        boolean flag = true;
        long ans = -1;
        for (int j = begin; j < end; j++) {
            if (predicate.test(values.get(j))) {
                if (sum) {
                    ans += j;
                } else {
                    if (flag) {
                        ans = j;
                        flag = false;
                    } else if (first ? j < ans : j > ans) {
                        ans = j;
                    }
                }
            }
        }
        return ans;
    }

    @Override
    public <T> int indexOf(int threads, List<T> values, Predicate<? super T> predicate) throws InterruptedException {
        return (int) parallelWork(true,threads, values, null, false, predicate, true, false);
    }

    @Override
    public <T> int lastIndexOf(int threads, List<T> values, Predicate<? super T> predicate) throws InterruptedException {
        return (int) parallelWork(true, threads, values, null, false, predicate, false, false);
    }

    @Override
    public <T> long sumIndices(int threads, List<? extends T> values, Predicate<? super T> predicate) throws InterruptedException {
        return parallelWork(true, threads, values, null, false, predicate, false, true);
    }
}
