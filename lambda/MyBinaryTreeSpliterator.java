package info.kgeorgiy.ja.Laskin_Pavel.lambda;

import info.kgeorgiy.java.advanced.lambda.Trees;

import java.util.*;
import java.util.function.Consumer;


public class MyBinaryTreeSpliterator<T> extends Spliterators.AbstractSpliterator<T> {

    private final Deque<Trees.Binary<T>> deq;

    protected MyBinaryTreeSpliterator(Trees.Binary<T> tree) {
        super(tree instanceof Trees.Leaf<T> ? 1 : Long.MAX_VALUE,
                tree instanceof Trees.Leaf<T> ?
                        Spliterator.SIZED | Spliterator.SUBSIZED | Spliterator.IMMUTABLE | Spliterator.ORDERED :
                        Spliterator.IMMUTABLE | Spliterator.ORDERED);
        deq = new ArrayDeque<>();
        deq.add(tree);
    }

    @Override
    public boolean tryAdvance(Consumer<? super T> action) {
        while (!deq.isEmpty()) {
            switch (deq.removeFirst()) {
                case Trees.Leaf<T> leaf -> {
                    if (leaf.value() != null) {
                        action.accept(leaf.value());
                        return true;
                    }
                    return false;
                }
                case Trees.Binary.Branch<T> branch -> {
                    if (branch.right() != null) {
                        deq.addFirst(branch.right());
                    }
                    if (branch.left() != null) {
                        deq.addFirst(branch.left());
                    }
                }
            }
        }
        return false;
    }



    @Override
    public Spliterator<T> trySplit() {
        if (!deq.isEmpty()) {
            switch (deq.removeFirst()) {
                case Trees.Leaf<T> leaf -> {
                    return null;
                }
                case Trees.Binary.Branch<T> branch -> {
                    if (branch.left() != null) {
                        if (branch.right() != null) {
                            deq.addFirst(branch.right());
                        }
                        return new MyBinaryTreeSpliterator<>(branch.left());
                    }
                    if (branch.right() != null) {
                        return new MyBinaryTreeSpliterator<>(branch.right());
                    }
                    return null;
                }
            }
        }
        return null;
    }

}