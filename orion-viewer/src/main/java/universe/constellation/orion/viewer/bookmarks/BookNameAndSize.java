package universe.constellation.orion.viewer.bookmarks;

public class BookNameAndSize implements Comparable<BookNameAndSize> {

    private final String name;

    private final long size;

    private long id;

    public BookNameAndSize(String name, long size) {
        this.name = name;
        this.size = size;
    }

    @Override
    public int compareTo(BookNameAndSize another) {
        int res = name.compareTo(another.name);
        if (res == 0) {
            res = Long.compare(size, another.size);
        }
        return res;
    }

    public String getName() {
        return name;
    }

    public long getSize() {
        return size;
    }

    public long getId() {
        return id;
    }

    public String beautifySize() {
        if (size < 1024) {
            return size + "b";
        }
        if (size < 1024 * 1024) {
            return (size / 1024) + "." + (size % 1024)/103 + "Kb";
        }
        if (size < 1024 * 1024 * 1024) {
            return (size / (1024 * 1024)) + "." + (size % (1024 * 1024))/(103*1024) + "Mb";
        }
        return size + "b";
    }

    @Override
    public String toString() {
        return name + " " + beautifySize();
    }

    @Override
    public boolean equals(Object o) {
        BookNameAndSize another = (BookNameAndSize) o;
        return size == another.size && name.equals(another.name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }
}
