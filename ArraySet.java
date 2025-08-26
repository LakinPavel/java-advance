package info.kgeorgiy.ja.Laskin_Pavel.arrayset;


import java.util.*;

public class ArraySet<T> extends AbstractSet<T> implements SortedSet<T> {

    private Comparator<? super T> comparator;
    private final List<T> listOfElements;

    private void CheckArgs(T fromElement, T toElement) throws IllegalArgumentException {
        if (comparator.compare(fromElement, toElement) > 0) {
            throw new IllegalArgumentException("Illegal arguments: left element greater than right");
        }
    }

    private void CheckCompr() {
        if (comparator == null) {
            comparator = Collections.reverseOrder().reversed();
        }
    }

    public ArraySet(Collection<? extends T> elements, Comparator<? super T> comparator) {
        this.comparator = comparator;
        TreeSet<T> elem = new TreeSet<>(comparator);
        elem.addAll(elements);
        listOfElements = new ArrayList<>(elem);
    }

    public ArraySet() {
        this(Collections.emptyList(), null);
    }

    public ArraySet(Collection<? extends T> elements) {
        this(elements, null);
    }

    public ArraySet(Comparator<? super T> comparator) {
        this(Collections.emptyList(), comparator);
    }

    public ArraySet(List<T> elements, int l, int r, Comparator<? super T> comparator) {
        this.comparator = comparator;
        int left = l;
        int right = r;
        if (l < 0) {
            left = -l - 1;
        }
        if (r < 0) {
            right = -r - 1;
        }
        listOfElements = elements.subList(left, right);
    }


    @Override
    public Iterator<T> iterator() {
        return Collections.unmodifiableList(listOfElements).iterator();
    }

    @Override
    public int size() {
        return listOfElements.size();
    }

    @Override
    public Comparator<? super T> comparator() {
        return comparator;
    }

    @Override
    public SortedSet<T> subSet(T fromElement, T toElement) {
        CheckCompr();
        CheckArgs(fromElement, toElement);
        return tailSet(fromElement).headSet(toElement);
    }

    @Override
    public SortedSet<T> headSet(T toElement) {
        int indTo = Collections.binarySearch(listOfElements, toElement, comparator);
        return new ArraySet<>(listOfElements, 0, indTo, comparator);
    }

    @Override
    public SortedSet<T> tailSet(T fromElement) {
        int indFrom = Collections.binarySearch(listOfElements, fromElement, comparator);
        return new ArraySet<>(listOfElements, indFrom, size(), comparator);
    }

    @Override
    public T first() {
        return listOfElements.getFirst();
    }

    @Override
    public T last() {
        return listOfElements.getLast();
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean contains(Object el) {
        return Collections.binarySearch(listOfElements, (T) el, comparator) >= 0;
    }
}