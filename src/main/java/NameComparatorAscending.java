import com.google.api.services.drive.model.File;
import java.util.Comparator;

public class NameComparatorAscending implements Comparator<Object> {

    @Override
    public int compare(Object f1, Object f2) {
        return ((File)f1).getName().compareTo(((File)f2).getName());
    }
}
