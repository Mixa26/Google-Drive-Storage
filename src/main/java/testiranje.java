import java.util.ArrayList;

public class testiranje {
    public static void main(String[] args) {
        ArrayList<String> forb = new ArrayList<>();
        //forb.add("txt");
        DriveStorage.getInstance().createRoot(new Configuration(100,5,forb));
        DriveStorage.getInstance().createDir("","folder");
        DriveStorage.getInstance().createDir("folder","folder1");
        String[] names = {"sa.txt", "da"};
        DriveStorage.getInstance().createFiles("folder",names);
        String[] paths = {"folder"};
        DriveStorage.getInstance().delete(paths);
    }
}
