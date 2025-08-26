package info.kgeorgiy.ja.Laskin_Pavel.lambda;

import info.kgeorgiy.java.advanced.lambda.Trees;

import java.util.*;
import java.util.function.Consumer;


public class MySizedBinaryTreeSpliterator<T> extends Spliterators.AbstractSpliterator<T> {

    private final Trees.SizedBinary<T> tree;

    private final Deque<Trees.SizedBinary<T>> deq;

    protected MySizedBinaryTreeSpliterator(Trees.SizedBinary<T> tree) {
        super(tree instanceof Trees.Leaf<T> ? 1 : Long.MAX_VALUE,  Spliterator.SIZED | Spliterator.IMMUTABLE | Spliterator.ORDERED);
        this.tree = tree;
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
                case Trees.SizedBinary.Branch<T> branch -> {
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
                case Trees.SizedBinary.Branch<T> branch -> {
                    if (branch.left() != null) {
                        if (branch.right() != null) {
                            deq.addFirst(branch.right());
                        }
                        return new MySizedBinaryTreeSpliterator<>(branch.left());
                    }
                    if (branch.right() != null) {
                        return new MySizedBinaryTreeSpliterator<>(branch.right());
                    }
                    return null;
                }
            }
        }
        return null;
    }

    @Override
    public long estimateSize(){
        switch (tree) {
            case Trees.Leaf<T> leaf -> {
                return leaf.size();
            }
            case Trees.SizedBinary.Branch<T> branch -> {
                return branch.left().size() + branch.right().size();
            }
            default -> { return 0; }
        }
    }
}