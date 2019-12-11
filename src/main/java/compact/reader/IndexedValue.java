package compact.reader;

public class IndexedValue <T> {
    public int index;
    public T batch;

    public IndexedValue(int index, T batch) {
        this.index = index;
        this.batch = batch;
    }
}
