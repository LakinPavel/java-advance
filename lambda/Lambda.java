package info.kgeorgiy.ja.Laskin_Pavel.lambda;

import info.kgeorgiy.java.advanced.lambda.EasyLambda;
import info.kgeorgiy.java.advanced.lambda.Trees;

import java.util.*;
import java.util.stream.Collector;

public class Lambda implements EasyLambda {

    @Override
    public <T> Spliterator<T> binaryTreeSpliterator(Trees.Binary<T> tree) {
        return new MyBinaryTreeSpliterator<>(tree);
    }


    @Override
    public <T> Spliterator<T> sizedBinaryTreeSpliterator(Trees.SizedBinary<T> tree) {
        return new MySizedBinaryTreeSpliterator<>(tree);
    }

    @Override
    public <T> Spliterator<T> naryTreeSpliterator(Trees.Nary<T> tree) {
        return new MyNaryTreeSpliterator<>(tree);
    }


    private <T> Collector<T, ?, Optional<T>> firstLast(String index) {
        int flag = index.equals("last") ? 1 : 0;
        return Collector.of(
                () -> new ArrayList<T>(1),
                (list, element) -> {
                    if (flag == 0) {
                        if (list.isEmpty()) {
                            list.add(element);
                        }
                    } else {
                        if (list.size() == 1) {
                            list.clear();
                        }
                        list.add(element);
                    }
                },
                (list1, list2) -> flag == 1 ? list2 : list1,
                list -> list.isEmpty() ? Optional.empty() : Optional.of(list.get(0))
        );
    }

    @Override
    public <T> Collector<T, ?, Optional<T>> first() {
        return firstLast("first");
    }

    @Override
    public <T> Collector<T, ?, Optional<T>> last() {
        return firstLast("last");
    }

    @Override
    public <T> Collector<T, ?, Optional<T>> middle() {
        return Collector.of(
                ArrayList<T>::new,
                List::add,
                (list1, list2) -> {
                    list1.addAll(list2);
                    return list1;
                },
                list -> list.isEmpty() ? Optional.empty() : Optional.of(list.get(list.size() / 2))
        );
    }

    private boolean checkEq(CharSequence cs1, CharSequence cs2, int i, int j) {
        return cs1.charAt(i) == cs2.charAt(j);
    }

    private int commonPart(CharSequence first, CharSequence second, String part) {
        int flag = part.equals("prefix") ? 1 : 0;
        int len1 = first.length();
        int len2 = second.length();
        int minLen = Math.min(len1, len2);
        if (flag == 1) {
            int i = 0;
            while (i < minLen && checkEq(first, second, i, i)) {
                i++;
            }
            return i;
        } else {
            int i = len1 - 1;
            int j = len2 - 1;
            while (i >= 0 && j >= 0 && checkEq(first, second, i, j)) {
                len1--;
                i--;
                j--;
            }
            return len1;
        }
    }

    // Could implement easier with using Collectors.collectingAndThen.reduce
    private Collector<CharSequence, ?, String> myCollector(String part) {
        boolean prefix = part.equals("prefix");
        return Collector.of(
                () -> new Mypair(new StringBuilder(), 0),
                (pair, charSq) -> {
                    if (pair.stringBuilder.isEmpty() && pair.value == 0) {
                        pair.value = 1;
                        pair.stringBuilder.append(charSq);
                    } else {
                        pair.value = 1;
                        int i = commonPart(pair.stringBuilder, charSq, part);
                        if (prefix) {
                            pair.stringBuilder.delete(i, pair.stringBuilder.length());
                        } else {
                            pair.stringBuilder.delete(0, i);
                        }

                    }
                },
                (pair1, pair2) -> {
                    if (pair1.stringBuilder.isEmpty()) {
                        pair1.stringBuilder.setLength(0);
                        if (!prefix) {
                            return pair1;
                        }
                    } else if (pair2.stringBuilder.isEmpty()) {
                        pair2.stringBuilder.setLength(0);
                        if (!prefix) {
                            return pair2;
                        }
                    } else {
                        int i = commonPart(pair1.stringBuilder, pair2.stringBuilder, part);
                        if (prefix) {
                            pair1.stringBuilder.delete(i, pair1.stringBuilder.length());
                        } else {
                            if (pair1.stringBuilder.length() < pair2.stringBuilder.length()) {
                                pair1.stringBuilder.delete(0, i);
                                return pair1;
                            }
                            pair2.stringBuilder.delete(0, i);
                            return pair2;
                        }
                    }
                    return pair1;
                },
                (sb) -> sb.stringBuilder.toString()
        );
    }

    @Override
    public Collector<CharSequence, ?, String> commonPrefix() {
        return myCollector("prefix");
    }

    @Override
    public Collector<CharSequence, ?, String> commonSuffix() {
        return myCollector("suffix");
    }
}
