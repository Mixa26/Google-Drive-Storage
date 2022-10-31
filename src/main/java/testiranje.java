import java.util.ArrayList;

public class testiranje {
    public static void main(String[] args) {
        ArrayList<String> forb = new ArrayList<>();
        //forb.add("txt");
        DriveStorage.getInstance().createRoot(new Configuration(100,5,forb));
        DriveStorage.getInstance().createDir("","folder");
        DriveStorage.getInstance().createDir("folder","folder1");
        DriveStorage.getInstance().createDir("folder","folder2");
        String[] names = {"sa.txt", "da"};
        String[] names1 = {"sab.txt"} ;
        DriveStorage.getInstance().createFiles("folder",names);
        DriveStorage.getInstance().createFiles("folder/folder2",names1);
        DriveStorage.getInstance().searchAllFilesInDirs("");
    }
}
