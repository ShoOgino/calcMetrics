package data;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import me.tongfei.progressbar.ProgressBar;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import static util.FileUtil.readFile;

public class Commits {
    private HashMap<String, Commit> commits = new HashMap<String, Commit>();
    public Commit get(String idCommit){
        return commits.get(idCommit);
    }
    public void loadCommits(String pathCommits) {
        //commits
        File fileCommits = new File(pathCommits);
        List<File> filesCommit = Arrays.asList(fileCommits.listFiles());
        for (File file : ProgressBar.wrap(filesCommit, "loadCommits")) {
            String path = file.toString();
            try {
                String strCommit = readFile(path);
                ObjectMapper mapper = new ObjectMapper();
                Commit commit = mapper.readValue(strCommit, new TypeReference<Commit>() {
                });
                commits.put(commit.id, commit);
            } catch (JsonParseException e) {
                e.printStackTrace();
            } catch (JsonMappingException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
