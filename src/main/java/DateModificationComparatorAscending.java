import com.google.api.services.drive.model.File;

import java.util.Comparator;
import java.util.Date;

public class DateModificationComparatorAscending implements Comparator<Object> {
    @Override
    public int compare(Object file1, Object file2) {
        Date date1 = new Date(((File)file1).getModifiedTime().getValue());
        Date date2 = new Date(((File)file2).getModifiedTime().getValue());
        return date1.compareTo(date2);
    }
}
