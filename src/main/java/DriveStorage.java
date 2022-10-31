import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.FileContent;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import error.StorageErrFactory;
import error.types.StorageErrorType;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import java.io.*;
import java.security.GeneralSecurityException;
import java.util.Arrays;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
    private String folderMimeType = "application/vnd.google-apps.folder";

    public Configuration currentDriveState;

    private Drive service;

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
        InputStream in = DriveQuickStart.class.getResourceAsStream(CREDENTIALS_FILE_PATH);
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

    @Override
    public boolean createRoot(Configuration configuration){
        try {
            //TODO proveri da li je root napravljen ukoliko neko hoce 2 roota da napravi
            final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();

            service = new Drive.Builder(HTTP_TRANSPORT, JSON_FACTORY, getCredentials(HTTP_TRANSPORT)).setApplicationName(APPLICATION_NAME).build();

            //creating a empty folder called Drive root
            File folderMetadata = new File();
            folderMetadata.setName("Drive root");
            folderMetadata.setMimeType("application/vnd.google-apps.folder");
            //setting this to be the root folder of the storage
            folderMetadata.setDriveId("root");

            java.io.File config = new java.io.File("files/configuration.json");

            //writing the contents of configuration into a jason file
            FileWriter writer = new FileWriter("files/configuration.json");
            writer.write(configuration.toJson());
            writer.close();

            //creating metadata for configuration for the drive on the cloud
            File fileMetadata = new File();
            fileMetadata.setName("configuration.json");
            FileContent configContent = new FileContent("media/text",config);

            //setting the Drives configuration
            //so we know the limitations
            driveConfiguration = configuration;
            currentDriveState = new Configuration(config.length(),1,new ArrayList<>());

            try{
                //setting up the folder and the configuration file for uploading to the drive cloud
                File folder = service.files().create(folderMetadata).setFields("id").execute();
                ArrayList<String> ParentsList = new ArrayList<>();
                rootId = folder.getId();
                ParentsList.add(rootId);
                fileMetadata.setParents(ParentsList);
                File file = service.files().create(fileMetadata, configContent).setFields("id, parents").execute();
                System.out.println("Root File Created with ID: " + folder.getId());
                return true;
            }
            catch (GoogleJsonResponseException e)
            {
                System.err.println("Something went wrong with Google Json respond.");
            }
        }
        catch (IOException | GeneralSecurityException e)
        {
            System.err.println("Something went wrong with preprocessing steps of converting the configuration file and creation of the Drive folder.");
        }
        return false;
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
            System.err.println("Error with getting files by name.");
        }
        return files;
    }

    private String getFileId(String path,String mimeiType, Drive service)
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
                StorageErrFactory.createError(StorageErrorType.NOT_A_DIRECTORY);
                System.err.println(path + " <-- bad path");
                return "";
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
                StorageErrFactory.createError(StorageErrorType.NOT_A_DIRECTORY);
                System.err.println(path + " <-- bad path");
                return "";
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
    public boolean createDir(String path, String name) {
        if (rootId.equals(""))
        {
            StorageErrFactory.createError(StorageErrorType.NO_ROOT);
            return false;
        }
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
                System.err.println("Error making a folder. Bad Json respond.");
            }
        }
        catch (IOException e)
        {
            System.err.println("Something went wrong with creating a directory.");
        }
        return false;
    }

    private boolean checkConfiguration(java.io.File file)
    {
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
        return true;
    }

    @Override
    public boolean createFiles(String path, String[] names) {
        for (int i = 0; i < names.length; i++)
        {
            java.io.File localFile = new java.io.File("files/" + names[i]);
            if (!localFile.exists()) {
                try {
                    if (!localFile.createNewFile()) {
                        System.err.println("Error creating the file: " + names[i]);
                        return false;
                    }
                } catch (IOException e) {
                    System.err.println("Error creating the file: " + names[i]);
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
                } catch (GoogleJsonResponseException e) {
                    System.err.println("Error encountered with Google's Json respond.");
                    System.err.println("File upload failed.");
                    return false;
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
            String pageToken = null;

            FileList result = service.files().list()
                    .setSpaces("drive")
                    .setFields("files(id, name, parents, size, mimeType, createdTime, modifiedTime)")
                    .execute();
            files = result.getFiles();

        }
        catch (IOException e)
        {
            System.err.println("Problem with searching all the files.");
        }
        return files;
    }

    @Override
    public boolean delete(String[] paths) {
        boolean valid = true;

        List<File> files = new ArrayList<File>();

        files = listAllFiles();

        if (files.isEmpty())
        {
            System.err.println("File deleting failed.");
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
                        currentDriveState.setBytes(currentDriveState.getBytes()-curr.getSize());
                        currentDriveState.setFiles(currentDriveState.getFiles()-1);
                    }
                }

                service.files().delete(deleteFileID).execute();
            } catch (IOException e) {
                System.err.println("Deleting file " + paths[i] + " failed.");
                valid = false;
            }
        }
        return valid;
    }

    @Override
    public boolean relocateFiles(String[] pathsFrom, String pathTo)
    {
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
                System.err.println("File " + pathsFrom[i] + " relocation failed.");
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
                System.err.println("Error encountered in communication with the remote.");
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean download(String pathFrom, String pathTo) {
        try {
            //TODO skidanje direktorijuma
            String fileID = getFileId(pathFrom,"",service);

            if (fileID.equals(""))
            {
                System.err.println("File download failed.");
                return false;
            }

            OutputStream outputStream = new ByteArrayOutputStream();

            service.files().get(fileID)
                    .executeMediaAndDownloadTo(outputStream);

            java.io.File file = new java.io.File(pathTo);
            InputStream input = new ByteArrayInputStream(((ByteArrayOutputStream)outputStream).toByteArray());

            FileUtils.copyInputStreamToFile(input, file);
        } catch (IOException e) {
            System.err.println("Error downloading the file. Either bad path or bad Json respond.");
            return false;
        }
        return true;
    }

    @Override
    public boolean rename(String path, String name) {

        String fileID = getFileId(path,"",service);

        if (fileID.equals(""))
        {
            System.err.println("Renaming the file failed.");
            return false;
        }

        try {
            File file = service.files().get(fileID)
                    .setFields("name")
                    .execute();

            file.setName(name);

            service.files().update(fileID,file).setFields("name").execute();
        } catch (IOException e) {
            System.err.println("Something went wrong with renaming the file.");
            return false;
        }
        return true;
    }

    @Override
    public Metadata searchAllFilesInDir(String dirPath) {
        String folderID = getFileId(dirPath,folderMimeType,service);

        if (folderID.equals(""))
        {
            System.err.println("File search failed.");
            return null;
        }

        List<File> files = new ArrayList<File>();
        files = listAllFiles();

        for (File file : files)
        {
            if (file.getParents() != null && file.getParents().contains(folderID))
            {
                System.out.printf("File: %s size: %s ctime: %s mtime: %s\n", file.getName(), file.getSize(), file.getCreatedTime(), file.getModifiedTime());
            }
        }

        return null;
    }

    @Override
    public ArrayList<java.io.File> searchAllDirsInDir(String dirPath) {
        String folderID = getFileId(dirPath,folderMimeType,service);

        if (folderID.equals(""))
        {
            System.err.println("File search failed.");
            return null;
        }

        List<File> files = new ArrayList<File>();
        files = listAllFiles();
        List<String> toSearch = new ArrayList<String>();

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
                        System.out.printf("File: %s size: %s ctime: %s mtime: %s\n", file.getName(), file.getSize(), file.getCreatedTime(), file.getModifiedTime());
                        break;
                    }
                }
            }
        }

        return null;
    }

    @Override
    public ArrayList<java.io.File> searchAllFilesInDirs(String s) {
        return null;
    }

    @Override
    public ArrayList<java.io.File> searchFilesByExt(String s, String s1) {
        return null;
    }

    @Override
    public ArrayList<java.io.File> searchFileBySub(String s) {
        return null;
    }

    @Override
    public boolean dirContainsFiles(String s, String[] strings) {
        return false;
    }

    @Override
    public java.io.File folderContainingFile(String s) {
        return null;
    }

    @Override
    public void sort(SortParamsEnum sortParamsEnum, boolean b) {

    }

    @Override
    public ArrayList<java.io.File> filesCreatedModifiedOnDate(String s) {
        return null;
    }

    @Override
    public void filterSearchResult(boolean b, boolean b1, boolean b2, boolean b3) {

    }
}
