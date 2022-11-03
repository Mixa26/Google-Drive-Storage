import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.FileContent;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import customExceptions.BadPathException;
import customExceptions.FileCreationException;
import customExceptions.NoConfigException;
import customExceptions.NoRootException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import java.io.*;
import java.security.GeneralSecurityException;
import java.util.*;

public class DriveStorage implements Storage{
    //configuring DriveStorage to be singleton
    private static DriveStorage instance;
    //root id which we use to identify the root folder
    //it is setup in the createRoot
    private String rootId = "";
    //setting the configuration(limitations)
    //so we know the max amount of files,bytes...
    private Configuration driveConfiguration;

    public static DriveStorage getInstance() {
        if (instance == null)
        {
            instance = new DriveStorage();
        }
        return instance;
    }

    private DriveStorage()
    {
    }

    //Person credential authorization
    /**
     * Application name.
     */
    private static final String APPLICATION_NAME = "Google Drive Storage";
    /**
     * Global instance of the JSON factory.
     */
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    /**
     * Directory to store authorization tokens for this application.
     */
    private static final String TOKENS_DIRECTORY_PATH = "tokens";

    /**
     * Global instance of the scopes required by this quickstart.
     * If modifying these scopes, delete your previously saved tokens/ folder.
     */
    private static final List<String> SCOPES = Arrays.asList(DriveScopes.DRIVE, DriveScopes.DRIVE_APPDATA, DriveScopes.DRIVE_FILE, DriveScopes.DRIVE_METADATA, DriveScopes.DRIVE_SCRIPTS);
    private static final String CREDENTIALS_FILE_PATH = "/credentials.json";

    //MY VARIABLES
    private final String folderMimeType = "application/vnd.google-apps.folder";


    private ArrayList<Configuration> fileConfigs = new ArrayList<>();

    private Drive service;

    private List<Object> lastSearchRes = new ArrayList<>();

    private String driveRootName = "Drive root";

    /**
     * Creates an authorized Credential object.
     *
     * @param HTTP_TRANSPORT The network HTTP Transport.
     * @return An authorized Credential object.
     * @throws IOException If the credentials.json file cannot be found.
     */
    private static Credential getCredentials(final NetHttpTransport HTTP_TRANSPORT)
            throws IOException {
        // Load client secrets.
        InputStream in = DriveStorage.class.getResourceAsStream(CREDENTIALS_FILE_PATH);
        if (in == null) {
            throw new FileNotFoundException("Resource not found: " + CREDENTIALS_FILE_PATH);
        }
        GoogleClientSecrets clientSecrets =
                GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

        // Build flow and trigger user authorization request.
        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
                .setDataStoreFactory(new FileDataStoreFactory(new java.io.File(TOKENS_DIRECTORY_PATH)))
                .setAccessType("offline")
                .build();
        LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8888).build();
        Credential credential = new AuthorizationCodeInstalledApp(flow, receiver).authorize("user2");
        //returns an authorized Credential object.
        return credential;
    }

    private List<File> getRootCreationFiles(String name)
    {
        FileList result;
        String nameProvide;
        nameProvide = "name='" + name + "'" + " and mimeType='application/vnd.google-apps.folder'";

        try {
            result = service.files().list()
                    .setQ(nameProvide)
                    .setSpaces("drive")
                    .setFields("files(id, name, parents, mimeType, size)")
                    .execute();
        }
        catch (IOException e)
        {
            e.printStackTrace();
            return new ArrayList<>();
        }
        return result.getFiles();
    }

    private String getRootCreationFolderID(String path)
    {
        String[] splits = path.split("/");
        String parentID = "";

        for (int i = 0; i < splits.length; i++)
        {
            List<File> folders = getRootCreationFiles(splits[i]);
            if (folders.isEmpty())
            {
                throw new BadPathException("Bad path!");
            }
            else
            {
                if (i != 0)
                {
                    for (int j = 0; j < folders.size(); j++)
                    {
                        if (folders.get(j).getParents().contains(parentID))
                        {
                            parentID = folders.get(j).getId();
                            break;
                        }
                    }
                }
                else
                {
                    parentID = folders.get(0).getId();
                }
            }
        }
        return parentID;
    }

    @Override
    public boolean createRoot(String path, Configuration configuration) throws BadPathException{
        try {
            //TODO proveri da li je root napravljen ukoliko neko hoce 2 roota da napravi
            final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();

            service = new Drive.Builder(HTTP_TRANSPORT, JSON_FACTORY, getCredentials(HTTP_TRANSPORT)).setApplicationName(APPLICATION_NAME).build();

            List<File> files = getRootCreationFiles(driveRootName);

            if (files.size() > 0)
            {
                rootId = files.get(0).getId();
                /*
                List<File> config = getRootCreationFiles("configuration.json");
                if (config.size() == 0)
                {
                    throw new NoConfigException("No config in root");
                }
                driveConfiguration = configuration.fromJson(conf)
                 */
                return true;
            }

            String fileID = "";

            if (!path.equals(""))
                fileID = getRootCreationFolderID(path);

            //creating a empty folder called Drive root
            File folderMetadata = new File();
            folderMetadata.setName(driveRootName);
            if (!fileID.equals(""))
            {
                folderMetadata.setParents(Collections.singletonList(fileID));
            }
            folderMetadata.setMimeType("application/vnd.google-apps.folder");
            //setting this to be the root folder of the storage

            java.io.File config = new java.io.File("files/configuration.json");

            //writing the contents of configuration into a jason file
            FileWriter writer = new FileWriter("files/configuration.json");
            fileConfigs.add(configuration);
            writer.write(configuration.toJson(fileConfigs));
            writer.close();

            //creating metadata for configuration for the drive on the cloud
            File fileMetadata = new File();
            fileMetadata.setName("configuration.json");
            FileContent configContent = new FileContent("media/text",config);

            try{
                //setting up the folder and the configuration file for uploading to the drive cloud
                File folder = service.files().create(folderMetadata).setFields("id").execute();
                ArrayList<String> ParentsList = new ArrayList<>();
                rootId = folder.getId();
                ParentsList.add(rootId);
                fileMetadata.setParents(ParentsList);
                File file = service.files().create(fileMetadata, configContent).setFields("id, parents").execute();
                //System.out.println("Root File Created with ID: " + folder.getId());
                return true;
            }
            catch (GoogleJsonResponseException e)
            {
                e.printStackTrace();
            }
        }
        catch (IOException | GeneralSecurityException e)
        {
            e.printStackTrace();
        }
        return false;
    }

    private void rootCheck() throws NoRootException
    {
        if (rootId.equals("")) {
            throw new NoRootException("No root created!");
        }
    }

    private List<File> getFilesByName(String name, String nameAndMimeiType, Drive service)
    {
        List<File> files = new ArrayList<File>();

        try {
            String pageToken = null;
            do {
                FileList result;

                if (name.equals("")) {
                    result = service.files().list()
                            .setQ(nameAndMimeiType)
                            .setSpaces("drive")
                            .setFields("nextPageToken, files(id, name, parents, mimeType, size)")
                            .setPageToken(pageToken)
                            .execute();
                } else {
                    result = service.files().list()
                            .setQ(name)
                            .setSpaces("drive")
                            .setFields("nextPageToken, files(id, name, parents, mimeType, size)")
                            .setPageToken(pageToken)
                            .execute();
                }

                files.addAll(result.getFiles());

                pageToken = result.getNextPageToken();
            } while (pageToken != null);
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        return files;
    }

    private String getFileId(String path,String mimeiType, Drive service) throws BadPathException
    {
        if (path.equals(""))return rootId;
        String[] folder = path.split("/");

        String parentID = rootId;

        boolean badBath = true;

        for (int i = 0; i < folder.length; i++)
        {
            String name = "name='" + folder[i] + "'";
            ArrayList<File> files;
            if (mimeiType != "") {
                String nameAndMimeiType = name + " and mimeType='" + mimeiType + "'";
                files = (ArrayList<File>) getFilesByName("", nameAndMimeiType, service);
            }
            else
            {
                files = (ArrayList<File>) getFilesByName(name,"", service);
            }
            if (files.isEmpty())
            {
                throw new BadPathException("Bad path!");
            }
            for (int j = 0; j < files.size(); j++)
            {
                if (files.get(j).getParents().contains(parentID))
                {
                    parentID = files.get(j).getId();
                    badBath = false;
                    break;
                }
            }
            if(badBath)
            {
                throw new BadPathException("Bad path!");
            }
            else
            {
                badBath = true;
            }
            files.clear();
        }

        return parentID;
    }

    @Override
    public boolean createDir(String path, String name) throws NoRootException{
        rootCheck();
        try {
            String fileId = getFileId(path,folderMimeType,service);

            File fileMetadata = new File();
            fileMetadata.setName(name);
            fileMetadata.setParents(Collections.singletonList(fileId));
            fileMetadata.setMimeType("application/vnd.google-apps.folder");
            try {
                File file = service.files().create(fileMetadata)
                        .setFields("id")
                        .execute();
                return true;
            } catch (GoogleJsonResponseException e) {
                e.printStackTrace();
            }
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        return false;
    }

    private boolean checkConfiguration(java.io.File file)
    {
        /*
        boolean valid = true;
        if (currentDriveState.getBytes() + file.length() > driveConfiguration.getBytes())
        {
            System.err.println("Configuration max bytes amount exceeded!");
            System.err.println("Error uploading the file.");
            valid = false;
        }
        if(currentDriveState.getFiles() + 1 > driveConfiguration.getFiles())
        {
            System.err.println("Configuration max files amount exceeded!");
            System.err.println("Error uploading the file.");
            valid = false;
        }
        if(driveConfiguration.getForbiddenExtensions().contains(FilenameUtils.getExtension(file.getPath()))) {
            System.err.println("Unsupported extension!");
            System.err.println("Error uploading the file.");
            valid = false;
        }
        if (!valid)return false;
        //updates the current drive state
        currentDriveState.setBytes(currentDriveState.getBytes()+file.length());
        currentDriveState.setFiles(currentDriveState.getFiles()+1);

         */
        return true;
    }

    @Override
    public boolean createFiles(String path, String[] names) throws FileCreationException{
        rootCheck();
        for (int i = 0; i < names.length; i++)
        {
            java.io.File localFile = new java.io.File("files/" + names[i]);
            if (!localFile.exists()) {
                try {
                    if (!localFile.createNewFile()) {
                        throw new FileCreationException("File not created.");
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    return false;
                }
            }
            if (checkConfiguration(localFile))
            {
                File fileMetadata = new File();
                fileMetadata.setName(names[i]);
                fileMetadata.setParents(Collections.singletonList(getFileId(path,folderMimeType,service)));
                FileContent mediaContent = new FileContent("media/text", localFile);
                try {
                    File file = service.files().create(fileMetadata, mediaContent)
                            .setFields("id, parents")
                            .execute();
                } catch (IOException e) {
                        e.printStackTrace();
                    return false;
                }
            }
            else
                return false;
        }
        return true;
    }

    private List<File> listAllFiles()
    {
        List<File> files = new ArrayList<File>();

        try {
            FileList result = service.files().list()
                    .setSpaces("drive")
                    .setFields("files(id, name, parents, size, mimeType, createdTime, modifiedTime, fileExtension)")
                    .execute();
            files = result.getFiles();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        return files;
    }

    private List<File> listAllDirs()
    {
        List<File> files = new ArrayList<File>();

        try {
            FileList result = service.files().list()
                    .setQ("mimeType='application/vnd.google-apps.folder'")
                    .setSpaces("drive")
                    .setFields("files(id, name, parents, size, mimeType, createdTime, modifiedTime, fileExtension)")
                    .execute();
            files = result.getFiles();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        return files;
    }

    @Override
    public boolean delete(String[] paths) {
        rootCheck();
        boolean valid = true;

        List<File> files = new ArrayList<File>();

        files = listAllFiles();

        if (files.isEmpty())
        {
            return false;
        }

        for (int i = 0; i < paths.length; i++)
        {
            String deleteFileID = getFileId(paths[i],"",service);

            try {
                for(int j = 0; j < files.size(); j++)
                {
                    File curr = files.get(j);
                    if (curr.getParents() == null)continue;
                    if (curr.getParents().contains(deleteFileID) && !curr.getMimeType().equals(folderMimeType))
                    {
                        //currentDriveState.setBytes(currentDriveState.getBytes()-curr.getSize());
                        //currentDriveState.setFiles(currentDriveState.getFiles()-1);
                    }
                }

                service.files().delete(deleteFileID).execute();
            } catch (IOException e) {
                e.printStackTrace();
                valid = false;
            }
        }
        return valid;
    }

    @Override
    public boolean relocateFiles(String[] pathsFrom, String pathTo)
    {
        rootCheck();
        String pathToID = getFileId(pathTo,folderMimeType,service);

        for (int i = 0; i < pathsFrom.length; i++)
        {
            String[] folderCuts = pathsFrom[i].split("/");
            StringBuilder folderPath = new StringBuilder();
            for (int j = 0; j < folderCuts.length-1; j++)
            {
                folderPath.append(folderCuts[j]);
                if (j<folderCuts.length-2)
                    folderPath.append("/");
            }
            String fileID = getFileId(pathsFrom[i],"",service);

            if (fileID.equals(""))
            {
                return false;
            }

            try
            {
                File file = service.files().get(fileID)
                        .setFields("id, name, parents")
                        .execute();
                StringBuilder previousParents = new StringBuilder();
                for (String parent : file.getParents()) {
                    previousParents.append(parent);
                    previousParents.append(',');
                }

                // Move the file to the new folder
                file = service.files().update(fileID, null)
                        .setAddParents(pathToID)
                        .setRemoveParents(previousParents.toString())
                        .setFields("id, parents")
                        .execute();
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean download(String pathFrom, String pathTo) throws UnsupportedOperationException{
        rootCheck();
        try {
            String fileID = getFileId(pathFrom,"",service);

            if (fileID.equals(""))
            {
                return false;
            }

            List<File> files = listAllFiles();
            for (File file : files)
            {
                if (file.getId().equals(fileID))
                {
                    if (file.getMimeType().equals(folderMimeType))
                    {
                        throw new UnsupportedOperationException();
                    }
                }
            }

            OutputStream outputStream = new ByteArrayOutputStream();

            service.files().get(fileID)
                    .executeMediaAndDownloadTo(outputStream);

            java.io.File file = new java.io.File(pathTo);
            InputStream input = new ByteArrayInputStream(((ByteArrayOutputStream)outputStream).toByteArray());

            FileUtils.copyInputStreamToFile(input, file);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    @Override
    public boolean rename(String path, String name) {
        rootCheck();
        String fileID = getFileId(path,"",service);

        if (fileID.equals(""))
        {
            return false;
        }

        try {
            File file = service.files().get(fileID)
                    .setFields("name")
                    .execute();

            file.setName(name);

            service.files().update(fileID,file).setFields("name").execute();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    @Override
    public ArrayList<Object> searchAllFilesInDir(String dirPath) {
        rootCheck();
        String folderID = getFileId(dirPath,folderMimeType,service);

        if (folderID.equals(""))
        {
            return new ArrayList<>();
        }

        List<File> files = new ArrayList<File>();
        files = listAllFiles();
        ArrayList<Object> res = new ArrayList<>();

        for (File file : files)
        {
            if (file.getParents() != null && file.getParents().contains(folderID))
            {
                res.add(file);
            }
        }
        lastSearchRes = res;
        return res;
    }

    @Override
    public ArrayList<Object> searchAllDirsInDir(String dirPath) {
        rootCheck();
        String folderID = getFileId(dirPath,folderMimeType,service);

        if (folderID.equals(""))
        {
            return new ArrayList<>();
        }

        List<File> files = new ArrayList<File>();
        files = listAllFiles();
        List<String> toSearch = new ArrayList<String>();
        ArrayList<Object> res = new ArrayList<>();

        for (File file : files)
        {
            if (file.getParents() != null && file.getParents().contains(folderID) && file.getMimeType().equals(folderMimeType))
            {
                toSearch.add(file.getId());
            }
        }
        for (File file : files)
        {
            if (file.getParents() != null)
            {
                for (int i = 0; i < toSearch.size(); i++)
                {
                    if (!file.getMimeType().equals(folderMimeType) && file.getParents().contains(toSearch.get(i)))
                    {
                        res.add(file);
                        break;
                    }
                }
            }
        }
        lastSearchRes = res;
        return res;
    }

    private List<Object> recursiveSubDirs(String ID, List<Object> files, List<File> allFiles)
    {
        for (int i = 0; i < allFiles.size(); i++)
        {
            File curr = allFiles.get(i);
            if (curr.getParents() != null && curr.getParents().contains(ID))
            {
                if (curr.getMimeType().equals(folderMimeType))
                {
                    files = recursiveSubDirs(curr.getId(), files, allFiles);
                }
                else
                    files.add(curr);
            }
        }
        return files;
    }

    @Override
    public ArrayList<Object> searchAllFilesInDirs(String path) {
        rootCheck();
        String folderID = getFileId(path,folderMimeType,service);
        List<File> allFiles = listAllFiles();
        List<Object> files = new ArrayList<>();

        if (folderID.equals(""))
        {
            return new ArrayList<>();
        }

        files = recursiveSubDirs(folderID, files, allFiles);

        lastSearchRes = files;
        return (ArrayList<Object>) files;
    }

    @Override
    public ArrayList<Object> searchFilesByExt(String path, String ext) {
        rootCheck();
        ArrayList<Object> res = new ArrayList<>();

        List<Object> files = searchAllFilesInDirs(path);

        for (int i = 0; i < files.size(); i++)
        {
            File curr = (File) files.get(i);
            if (curr.getFileExtension() != null && curr.getFileExtension().equals(ext))
            {
                res.add(curr);
            }
        }
        lastSearchRes = res;
        return res;
    }

    @Override
    public ArrayList<Object> searchFileBySub(String sub) {
        rootCheck();
        ArrayList<Object> res = new ArrayList<>();

        List<Object> files = searchAllFilesInDirs("");

        for (int i = 0; i < files.size(); i++)
        {
            File curr = (File) files.get(i);
            if (curr.getName().contains(sub))
            {
                res.add(curr);
            }
        }
        lastSearchRes = res;
        return res;
    }

    @Override
    public boolean dirContainsFiles(String path, String[] names) {
        rootCheck();
        List<Object> files = searchAllFilesInDirs(path);
        List<String> fileNames = new ArrayList<>();

        for (Object file : files)
        {
            fileNames.add(((File)file).getName());
        }

        for (String name : names)
        {
            if (!fileNames.contains(name))
            {
                return false;
            }
        }

        return true;
    }

    @Override
    public String folderContainingFile(String name) throws FileNotFoundException{
        rootCheck();
        List<File> allFolders = listAllDirs();
        List<File> allFiles = listAllFiles();

        File fileSearchedFor = null;

        for (File file : allFiles)
        {
            if (file.getName().equals(name))
            {
                fileSearchedFor = file;
                break;
            }
        }

        if (fileSearchedFor == null)
        {
            throw new FileNotFoundException();
        }

        for (File folder : allFolders)
        {
            if (fileSearchedFor.getParents() != null && fileSearchedFor.getParents().contains(folder.getId()))
            {
                return folder.getName();
            }
        }

        throw new FileNotFoundException();
    }

    @Override
    public void sort(SortParamsEnum sortParamsEnum, boolean ascending) {
        rootCheck();
        List<Object> files = new ArrayList<>();

        for (Object file : lastSearchRes)
        {
            files.add((File)file);
        }

        if (sortParamsEnum.equals(SortParamsEnum.NAME))
        {
            if (ascending)
                files.sort(new NameComparatorAscending());
            else
                files.sort(new NameComparatorDescending());

            lastSearchRes = files;
        }
        else if (sortParamsEnum.equals(SortParamsEnum.DATE_OF_CREATION))
        {
            if (ascending)
                files.sort(new DateCreationComparatorAscending());
            else
                files.sort(new DateCreationComparatorDescending());

            lastSearchRes = files;
        }
        else if (sortParamsEnum.equals(SortParamsEnum.DATE_OF_MODIFICATION))
        {
            if (ascending)
                files.sort(new DateModificationComparatorAscending());
            else
                files.sort(new DateModificationComparatorDescending());

            lastSearchRes = files;
        }
    }

    @Override
    public ArrayList<Object> filesCreatedModifiedOnDate(Date date, Date date1) {
        rootCheck();
        List<Object> files = searchAllFilesInDirs("");
        ArrayList<Object> res = new ArrayList<>();

        for (Object file : files)
        {
            System.out.println(((File)file).getCreatedTime().getValue());
            Date cFileDate = new Date(((File)file).getCreatedTime().getValue());
            Date mFileDate = new Date(((File)file).getModifiedTime().getValue());

            if ((date.compareTo(cFileDate) < 0 && cFileDate.compareTo(date1) < 0) || (date.compareTo(mFileDate) < 0 && mFileDate.compareTo(date1) < 0))
                res.add(file);
        }
        lastSearchRes = res;
        return res;
    }

    private StringBuilder constructFullPath(File file, StringBuilder res, List<File> allDirs)
    {
        for (File dir : allDirs)
        {
            if (dir.getId().equals(rootId))break;
            if (file.getParents() != null && file.getParents().contains(dir.getId()))
            {
                res.insert(0,dir.getName() + "/");
                res = constructFullPath(dir, res, allDirs);
            }
        }
        return res;
    }

    @Override
    public void filterSearchResult(boolean fullPath, boolean showSize, boolean showDateOfCreation, boolean showDateOfModification) {
        rootCheck();
        ArrayList<String> filesFiltered = new ArrayList<>();
        List<File> allDirs = listAllDirs();

        int minSpaces = 0;
        if (fullPath)
        {
            for (Object file : lastSearchRes)
            {
                StringBuilder sb = new StringBuilder();
                sb.append(((File)file).getName());
                sb = constructFullPath(((File)file),sb,allDirs);
                String FULL_PATH = sb.toString();
                if (FULL_PATH.length() > minSpaces)
                    minSpaces = FULL_PATH.length();
                filesFiltered.add(FULL_PATH);
            }
        }
        else
        {
            for (Object file : lastSearchRes)
            {
                filesFiltered.add(((File)file).getName());
            }
        }

        for (int i = 0; i < filesFiltered.size(); i++)
        {
            StringBuilder sb = new StringBuilder();
            sb.append(filesFiltered.get(i));
            for (int j = 0; j < minSpaces - filesFiltered.get(i).length(); j++)
            {
                sb.append(" ");
            }
            filesFiltered.set(i, sb.toString());
        }

        if (showSize)
        {
            for (int i = 0; i < lastSearchRes.size(); i++)
            {
                StringBuilder sb = new StringBuilder();
                sb.append(filesFiltered.get(i));
                sb.append(" ");
                sb.append(((File) lastSearchRes.get(i)).getSize()/1024);
                sb.append("KB");
                filesFiltered.set(i, sb.toString());
            }
        }
        if (showDateOfCreation)
        {
            for (int i = 0; i < lastSearchRes.size(); i++)
            {
                StringBuilder sb = new StringBuilder();
                sb.append(filesFiltered.get(i));
                sb.append(" date created: ");
                sb.append(new Date(((File) lastSearchRes.get(i)).getCreatedTime().getValue()).toString());
                filesFiltered.set(i, sb.toString());
            }
        }
        if (showDateOfModification)
        {
            for (int i = 0; i < lastSearchRes.size(); i++)
            {
                StringBuilder sb = new StringBuilder();
                sb.append(filesFiltered.get(i));
                sb.append(" date modified: ");
                sb.append(new Date(((File) lastSearchRes.get(i)).getModifiedTime().getValue()).toString());
                filesFiltered.set(i, sb.toString());
            }
        }
        for (String file : filesFiltered)
        {
            System.out.println(file);
        }
    }

    @Override
    public void printRes(ArrayList<Object> arrayList) {
        for (Object file : arrayList)
        {
            System.out.println(((File)file).getName());
        }
    }
}
