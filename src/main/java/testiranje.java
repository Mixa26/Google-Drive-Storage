import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;

public class testiranje {
    public static void main(String[] args) {
        ArrayList<String> forb = new ArrayList<>();
        //forb.add("txt");
        DriveStorage.getInstance().createRoot(new Configuration(100,5,forb));
        DriveStorage.getInstance().createDir("","folder");
        DriveStorage.getInstance().createDir("folder","folder1");
        DriveStorage.getInstance().createDir("folder","folder2");
        String[] names = {"a.txt", "b.txt"};
        String[] names1 = {"c.txt"};
        String[] names3 = {"k.txt"};
        DriveStorage.getInstance().createFiles("folder",names);
        DriveStorage.getInstance().createFiles("folder/folder1",names1);
        DriveStorage.getInstance().createFiles("folder/folder2",names3);
        DriveStorage.getInstance().searchFilesByExt("","txt");
        DriveStorage.getInstance().sort(SortParamsEnum.DATE_OF_CREATION, false);
        DriveStorage.getInstance().filterSearchResult(true,false,true,false);
    }
}
