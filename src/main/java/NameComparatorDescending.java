import com.google.api.services.drive.model.File;
import java.util.Comparator;

public class NameComparatorDescending implements Comparator<Object> {

    @Override
    public int compare(Object f1, Object f2) {
        return ((File)f2).getName().compareTo(((File)f1).getName());
    }
}
