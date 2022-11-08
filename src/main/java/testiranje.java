import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;

public class testiranje {
    public static void main(String[] args) {
        ArrayList<String> forb = new ArrayList<>();
        //forb.add("txt");
        DriveStorage.getInstance().createRoot("d/a/DriveRoot", null);
        //DriveStorage.getInstance().createDir("","folder",new Configuration("folder", 100, 1, forb));
        //DriveStorage.getInstance().createDir("folder","folder1",new Configuration("folder1", 100, 4, forb));
        //DriveStorage.getInstance().createDir("folder","folder2",null);
        String[] names = {"folder1/c.txt"};
        String[] names1 = {"c.txt"};
        String[] names2 = {"k.txt"};
        String[] names3 = {"folder"};
        DriveStorage.getInstance().download("folder/a.txt","files/c.txt");
        //DriveStorage.getInstance().createFiles("folder",names);
        //DriveStorage.getInstance().rename("folder","files");
    }
}
