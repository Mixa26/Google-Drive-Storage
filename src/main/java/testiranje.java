import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;

public class testiranje {
    public static void main(String[] args) {
        ArrayList<String> forb = new ArrayList<>();
        //forb.add("txt");
        DriveStorage.getInstance().createRoot("d/a/DriveRoot", new Configuration("DriveRoot",100,4,forb));
        //DriveStorage.getInstance().createDir("","folder",new Configuration("folder", 100, 4, forb));
        //DriveStorage.getInstance().createDir("folder","folder1",new Configuration("folder1", 100, 4, forb));
        //DriveStorage.getInstance().createDir("folder","folder2",null);
        String[] names = {"a.txt", "b.txt"};
        String[] names1 = {"c.txt"};
        String[] names2 = {"k.txt"};
        String[] names3 = {"folder"};
        //DriveStorage.getInstance().createFiles("folder/folder1",names);
        //DriveStorage.getInstance().rename("folder","files");
    }
}
