public interface GitInterface {
    void stage(String filePath);

    String commit(String author, String message);

}