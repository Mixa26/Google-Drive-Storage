import java.util.ArrayList;

public class testiranje {
    public static void main(String[] args) {
        DriveStorage.getInstance().createRoot(new Configuration(100,2,new ArrayList<>()));
        DriveStorage.getInstance().createDir("","folder");
        DriveStorage.getInstance().createDir("folder","folder1");
        String[] names = {"sa.txt", "da"};
        DriveStorage.getInstance().createFiles("folder",names);
    }
}
