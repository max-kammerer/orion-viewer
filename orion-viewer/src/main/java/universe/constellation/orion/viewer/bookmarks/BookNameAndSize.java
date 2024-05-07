package universe.constellation.orion.viewer.bookmarks;

import universe.constellation.orion.viewer.FileUtil;

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
        return FileUtil.beautifyFileSize(size);
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
