package info.kgeorgiy.ja.Laskin_Pavel.lambda;

import info.kgeorgiy.java.advanced.lambda.Trees;

import java.util.*;
import java.util.function.Consumer;


public class MyNaryTreeSpliterator<T> extends Spliterators.AbstractSpliterator<T> {

    private final Deque<Trees.Nary<T>> deq;

    protected MyNaryTreeSpliterator(Trees.Nary<T> tree) {
        super(tree instanceof Trees.Leaf<T> ? 1 : Long.MAX_VALUE,
                tree instanceof Trees.Leaf<T> ?
                        Spliterator.SIZED | Spliterator.SUBSIZED | Spliterator.IMMUTABLE | Spliterator.ORDERED :
                        Spliterator.IMMUTABLE | Spliterator.ORDERED);
        deq = new ArrayDeque<>();
        deq.add(tree);
    }

    @Override
    public boolean tryAdvance(Consumer<? super T> action) {
        long len = deq.size();
        while (len != 0) {
            switch (deq.removeFirst()) {
                case Trees.Leaf<T> leaf -> {
                    if (leaf.value() != null) {
                        action.accept(leaf.value());
                        return true;
                    }
                    return false;
                }
                case Trees.Nary.Node<T> node -> {
                        for (int i = node.children().size() - 1; i >= 0;  i--){
                            deq.addFirst(node.children().get(i));
                            len += 1;
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
                case Trees.Nary.Node<T> node -> {
                            for (int i = node.children().size() - 1; i >= 1; i--){
                                deq.addFirst(node.children().get(i));
                            }
                        return new MyNaryTreeSpliterator<>(node.children().get(0));
                }
            }
        }
        return null;
    }
}
