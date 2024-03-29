package adk.launcher;

import adk.team.Team;
import adk.team.yrescue.YRescueTeam;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

public class TeamLoader {

    public static final String KEYWORD_RANDOM = "random";
    private Random random;

    //team module
    private Map<String, Team> teamMap;
    private List<String> teamNameList;
    private Team default_team;
    
    public TeamLoader(File loadFile) {
        this.teamMap = new HashMap<>();
        this.teamNameList = new ArrayList<>();
        this.random = new Random((new Date()).getTime());
        this.default_team = new YRescueTeam();
        this.load(loadFile);
    }

    public Team get(String name) {
        return KEYWORD_RANDOM.equalsIgnoreCase(name) ? this.getRandomTeam() : this.getTeam(name);
    }

    public Team getTeam(String name) {
        return this.teamMap.get(name);
    }

    public Team getRandomTeam() {
        return teamNameList.isEmpty() ? null : this.teamMap.get(this.teamNameList.get(this.random.nextInt(this.teamNameList.size())));
    }

    public int size() {
        return teamNameList.size();
    }

    public Team getDefaultTeam() {
        return this.default_team;
    }

    private void load(File loadFile) {
        System.out.println("[START] Load Jar (path:" + loadFile.getAbsolutePath() + ")");
        if (!loadFile.exists()) {
            if(loadFile.mkdir()) {
                System.out.println("[END  ] Load Jar");
                return;
            }
            else {
                System.out.println("[ERROR] Cannot Create Directory (" + loadFile.getAbsolutePath() + ")");
                System.out.println("[END  ] Load Jar");
                return;
            }
        }

        URLClassLoader loader = (URLClassLoader) this.getClass().getClassLoader();
        List<String> list = new ArrayList<>();
        this.loadJar(loadFile, loader, list);
        this.loadTeam(loader, list);
        if(this.teamNameList.isEmpty()) {
            String name = this.default_team.getTeamName();
            this.teamMap.put(name, this.default_team);
            this.teamNameList.add(name);
        }
        System.out.println("[END  ] Load Jar");
    }

    private void loadJar(File loadFile, URLClassLoader loader, List<String> list) {
        if(loadFile.isDirectory()) {
            File[] files = loadFile.listFiles();
            if(files != null) {
                for (File file : files) {
                    this.loadJar(file, loader, list);
                }
            }
        }
        else if (loadFile.getName().endsWith(".jar")) {
            System.out.println("[INFO ] Found Jar (jarName:" + loadFile.getName() + ")");
            try {
                //add url
                Method method = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
                method.setAccessible(true);
                method.invoke(loader, loadFile.toURI().toURL());
                //load util class name
                JarFile jar = new JarFile(loadFile);
                Manifest manifest = jar.getManifest();
                Attributes attributes = manifest.getMainAttributes();
                String target = attributes.getValue("Team-Class");
                if(target != null) {
                    System.out.println("[INFO ] Found Target Class (targetClass:" + target + ")");
                    list.add(target);
                }
            } catch (NoSuchMethodException | IOException | IllegalAccessException | InvocationTargetException e) {
                e.printStackTrace();
            }
        }
    }

    private void loadTeam(URLClassLoader loader, List<String> list) {
        for (String target : list) {
            try {
                Class teamClass = loader.loadClass(target);
                Object obj = teamClass.newInstance();
                if (obj instanceof Team) {
                    Team team = (Team) obj;
                    String name = team.getTeamName();
                    System.out.println("[INFO ] Load Success (teamName:" + name + ")");
                    this.teamNameList.add(name);
                    this.teamMap.put(name, team);
                }
            } catch (ClassNotFoundException | IllegalAccessException | InstantiationException e) { //loadClass
                e.printStackTrace();
            }
        }
    }
}
