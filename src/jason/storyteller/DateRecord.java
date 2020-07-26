package jason.storyteller;

import org.json.JSONObject;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;

public class DateRecord {
    private JSONObject dateRecordJson;
    private final String scene = "0";

    private final ArrayList<Snippet> snippets = new ArrayList<>();

    public DateRecord(String date, SnippetFilter filter) {
        InputStream inputStream;

        try {
            inputStream = getClass().getResourceAsStream(String.format("snippets/%s.json", date));
        } catch (Exception e) {
            throw new IllegalArgumentException(String.format("Date %s does not exist.", date));
        }
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        StringBuilder contentBuilder = new StringBuilder();

        reader.lines().forEach(s -> contentBuilder.append(s).append("\n"));

        dateRecordJson = new JSONObject(contentBuilder.toString());

        /*JSONObject header = json.getJSONObject("header");
        if (header.getInt("episode") != episode || header.getInt("act") != act){
            throw new IllegalArgumentException(String.format("Scene %s is not ep %s, act %s", date, episode, act));
        }*/

        JSONObject scene;
        for (String sceneKey:dateRecordJson.getJSONObject("scenes").keySet()) {
            scene = dateRecordJson.getJSONObject("scenes").getJSONObject(sceneKey);

            if (checkSnippetTags(filter, new ArrayList<>(Arrays.asList(scene
                            .optJSONObject("header").getString("tags").split(", "))))) {
                snippets.add(new Snippet(scene));
            }
        }
    }

    public int countSnippets(){
        return snippets.size();
    }

    public Snippet getSnippet(int index){
        return snippets.get(index);
    }

    public boolean checkSnippetTags(SnippetFilter filter, ArrayList<String> sceneTags) {
        if (sceneTags.stream().anyMatch((filter.getExcludedTags()::contains))) {
            return false;
        }
        if (filter.isAny()) {
            return sceneTags.stream().anyMatch(filter.getIncludedTags()::contains);
        } else {
            return sceneTags.containsAll(filter.getIncludedTags());
        }
    }

    public class Snippet implements Iterable<JSONObject> {
        ArrayList<MiniActor> cast = new ArrayList<>();
        JSONObject snippetJson;

        public Snippet(JSONObject snippetJson){
            this.snippetJson = snippetJson;

            JSONObject actors = snippetJson.getJSONObject("header").optJSONObject("actors");
            if (actors != null) {
                actors.keySet().forEach(a -> cast.add(new MiniActor(a, actors.getJSONObject(a))));
            }
        }

        public int getSnippetLength() {
            return snippetJson.getJSONObject("header").getInt("length");
        }

        /**
         * Returns an iterator over elements of type {@code String[]}.
         *
         * @return an Iterator.
         */
        @Override
        public Iterator<JSONObject> iterator() {
            return new SnippetIterator();
        }

        public MiniActor getActor(String name) {
            for (MiniActor actor:cast) {
                if (actor.getName().equals(name)) {
                    return actor;
                }
            }
            return null;
        }

        class MiniActor {
            /**
             * The actor's name.
             */
            private String name;
            /**
             * The actor's speaking color.
             */
            private String color;
            /**
             * The side of the screen the actor's messages show up on. {@code true} is left, {@code false} is right.
             */
            private boolean forwards;

            public MiniActor(String name, JSONObject json) {
                this.name = name;
                color = ANSI.dynamicColor.get(json.getString("color"));
                forwards = json.getString("side").equals("Right");
            }

            public String getName() {
                return name;
            }

            public void setName(String name) {
                this.name = name;
            }

            public String getColor() {
                return color;
            }

            public void setColor(String color) {
                this.color = color;
            }

            public boolean isForwards() {
                return forwards;
            }

            public void setForwards(boolean forwards) {
                this.forwards = forwards;
            }
        }

        class SnippetIterator implements Iterator<JSONObject> {
            private final int totalLines;
            private int currentLine;

            public SnippetIterator() {
                totalLines = snippetJson.length();
                currentLine = 1;
            }

            /**
             * Returns {@code true} if the iteration has more elements.
             * (In other words, returns {@code true} if {@link #next} would
             * return an element rather than throwing an exception.)
             *
             * @return {@code true} if the iteration has more elements
             */
            @Override
            public boolean hasNext() {
                return currentLine < totalLines;
            }

            /**
             * Returns the next element in the iteration.
             *
             * @return the next element in the iteration
             * @throws NoSuchElementException if the iteration has no more elements
             */
            @Override
            public JSONObject next() {
                JSONObject line = snippetJson.getJSONObject(String.valueOf(currentLine));
                currentLine++;
                return line;
            }
        }
    }
}
