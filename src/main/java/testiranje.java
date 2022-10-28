public class testiranje {
    public static void main(String[] args) {
        DriveStorage.getInstance().createRoot(new Configuration());
        DriveStorage.getInstance().createDir("","folder");
        DriveStorage.getInstance().createDir("folder","folder1");
        DriveStorage.getInstance().createDir("folder","folder2");
        DriveStorage.getInstance().createDir("folder/folder2","folder3");
    }
}
