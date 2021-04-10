package data;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.HashMap;

import static util.FileUtil.readFile;

public class ModulesAll {
    HashMap<String, Module> modules = new HashMap<String, Module>();
    public Module get(String path){
        return modules.get(path);
    }
    public void loadModules(String pathModules){
        //module
        try {
            String strModule=readFile(pathModules);
            ObjectMapper mapper = new ObjectMapper();
            modules = mapper.readValue(strModule, new TypeReference<HashMap<String, Module>>() {});
        } catch (JsonParseException e) {
            e.printStackTrace();
        } catch (JsonMappingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

