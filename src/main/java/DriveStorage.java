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
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;

import java.io.*;
import java.security.GeneralSecurityException;
import java.util.Arrays;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DriveStorage implements Storage{
    //configuring DriveStorage to be singleton
    private static DriveStorage instance;
    private String rootId = "";

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
        Credential credential = new AuthorizationCodeInstalledApp(flow, receiver).authorize("user1");
        //returns an authorized Credential object.
        return credential;
    }

    @Override
    public boolean createRoot(Configuration configuration){
        try {
            final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();

            Drive service = new Drive.Builder(HTTP_TRANSPORT, JSON_FACTORY, getCredentials(HTTP_TRANSPORT)).setApplicationName(APPLICATION_NAME).build();

            //creating a empty folder called Drive root
            File folderMetadata = new File();
            folderMetadata.setName("Drive root");
            folderMetadata.setMimeType("application/vnd.google-apps.folder");
            //setting this to be the root folder of the storage
            folderMetadata.setDriveId("root");

            java.io.File config = new java.io.File("files/configuration.json");
            //config.createNewFile();

            FileWriter writer = new FileWriter("files/configuration.json");
            writer.write(configuration.toJson());
            writer.close();
            
            File fileMetadata = new File();
            fileMetadata.setName("configuration.json");
            FileContent configContent = new FileContent("media/text",config);

            try{
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
                //TODO uradi exception handling ovde
                System.err.println("Unable to upload file: " + e.getDetails());
                throw e;
            }
        }
        catch (IOException | GeneralSecurityException e)
        {
            //TODO uradi exception handling ovde
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public boolean createDir(String path, String name) {
        return false;
    }

    @Override
    public boolean saveFile(String[] pathsFrom, String pathTo) {
        return false;
    }

    @Override
    public boolean deleteFile(String[] paths) {
        return false;
    }

    @Override
    public boolean deleteDir(String[] dirs) {
        return false;
    }

    @Override
    public boolean relocateFiles(String[] paths) {
        return false;
    }

    @Override
    public boolean download(String[] pathsFrom, String pathTo) {
        return false;
    }

    @Override
    public boolean rename(String name) {
        return false;
    }

    @Override
    public Metadata searchAllFilesInDir(String dirPath) {
        return null;
    }

    @Override
    public ArrayList<java.io.File> searchAllDirsInDir(String dirPath) {
        return null;
    }

    @Override
    public ArrayList<java.io.File> searchAllFilesInDirs(String dirPath) {
        return null;
    }

    @Override
    public ArrayList<java.io.File> searchFilesByExt(String ext) {
        return null;
    }

    @Override
    public ArrayList<java.io.File> searchFileBySub(String substring) {
        return null;
    }

    @Override
    public boolean dirContainsFiles(String[] names) {
        return false;
    }

    @Override
    public java.io.File folderContainingFile(String name) {
        return null;
    }

    @Override
    public void sort(SortParamsEnum sortBy, boolean ascending) {

    }

    @Override
    public ArrayList<java.io.File> filesCreatedModifiedOnDate(String dirPath) {
        return null;
    }

    @Override
    public void filterSearchResult(boolean fullPath, boolean showSize, boolean showDateOfCreation, boolean showDateOfModification) {

    }
}
