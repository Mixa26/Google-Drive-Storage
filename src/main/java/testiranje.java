import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;

public class testiranje {
    public static void main(String[] args) {
        ArrayList<String> forb = new ArrayList<>();
        //forb.add("txt");
        DriveStorage.getInstance().createRoot("", new Configuration("root",100,2,forb));
        DriveStorage.getInstance().createDir("","folder",null);
        DriveStorage.getInstance().createDir("folder","folder1",null);
        DriveStorage.getInstance().createDir("folder","folder2",null);
        String[] names = {"a.txt", "b.txt"};
        String[] names1 = {"c.txt"};
        String[] names2 = {"k.txt"};
        String[] names3 = {"folder"};
        DriveStorage.getInstance().createFiles("folder",names);
        DriveStorage.getInstance().rename("folder","files");
    }
}
