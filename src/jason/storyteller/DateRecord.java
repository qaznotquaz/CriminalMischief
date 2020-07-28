package jason.storyteller;

import jason.terminal.ANSI;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * A wrapper for a .json containing all snippets of a certain date.
 */
public class DateRecord {
    /**
     * All snippets that fit the filter given.
     */
    private final ArrayList<Snippet> snippets = new ArrayList<>();

    /**
     * Instantiates a new Date record.
     *
     * @param date   the date to search for records from
     * @param filter the filter to use to search through each snippet's tags
     */
    public DateRecord(String date, SnippetFilter filter) {
        InputStream inputStream;

        // we need to use this rather lengthy few commands because we're technically not accessing a file (since we're looking in a .jar)
        try {
            // all snippets are contained in the snippets directory, and are named according to 'm.d.-y.json'
            inputStream = getClass().getResourceAsStream(String.format("snippets/%s.json", date));
        } catch (Exception e) {
            throw new IllegalArgumentException(String.format("Date %s does not exist.", date));
        }

        // if we've found a file, we read through it and convert it into a JSONObject
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        StringBuilder contentBuilder = new StringBuilder();
        reader.lines().forEach(s -> contentBuilder.append(s).append("\n"));
        JSONObject dateRecordJson = new JSONObject(contentBuilder.toString());

        // once we have the json object,
        JSONObject scene;
        for (String sceneKey: dateRecordJson.getJSONObject("scenes").keySet()) {
            scene = dateRecordJson.getJSONObject("scenes").getJSONObject(sceneKey);

            if (checkSnippetTags(filter, new ArrayList<>(Arrays.asList(scene
                            .optJSONObject("header").getString("tags").split(", "))))) {
                snippets.add(new Snippet(scene));
            }
        }
    }

    /**
     * Count snippets int.
     *
     * @return the int
     */
    public int countSnippets(){
        return snippets.size();
    }

    /**
     * Get snippet snippet.
     *
     * @param index the index
     * @return the snippet
     */
    public Snippet getSnippet(int index){
        return snippets.get(index);
    }

    /**
     * Check snippet tags boolean.
     *
     * @param filter    the filter
     * @param sceneTags the scene tags
     * @return the boolean
     */
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

    /**
     * The type Snippet.
     */
    public static class Snippet implements Iterable<JSONObject> {
        /**
         * The Cast.
         */
        ArrayList<MiniActor> cast = new ArrayList<>();
        /**
         * The Snippet json.
         */
        JSONObject snippetJson;

        /**
         * Instantiates a new Snippet.
         *
         * @param snippetJson the snippet json
         */
        public Snippet(JSONObject snippetJson){
            this.snippetJson = snippetJson;

            JSONObject actors = snippetJson.getJSONObject("header").optJSONObject("actors");
            if (actors != null) {
                actors.keySet().forEach(a -> cast.add(new MiniActor(a, actors.getJSONObject(a))));
            }
        }

        /**
         * Gets snippet length.
         *
         * @return the snippet length
         */
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

        /**
         * Gets actor.
         *
         * @param name the name
         * @return the actor
         */
        public MiniActor getActor(String name) {
            for (MiniActor actor:cast) {
                if (actor.getName().equals(name)) {
                    return actor;
                }
            }
            return null;
        }

        public String getTimestamp() {
            return snippetJson.getJSONObject("header").getString("timestamp");
        }

        /**
         * The type Mini actor.
         */
        @SuppressWarnings("unused")
        static
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

            /**
             * Instantiates a new Mini actor.
             *
             * @param name the name
             * @param json the json
             */
            public MiniActor(String name, JSONObject json) {
                this.name = name;
                color = ANSI.dynamicColor.get(json.getString("color"));
                forwards = json.getString("side").equals("Right");
            }

            /**
             * Gets name.
             *
             * @return the name
             */
            public String getName() {
                return name;
            }

            /**
             * Sets name.
             *
             * @param name the name
             */
            public void setName(String name) {
                this.name = name;
            }

            /**
             * Gets color.
             *
             * @return the color
             */
            public String getColor() {
                return color;
            }

            /**
             * Sets color.
             *
             * @param color the color
             */
            public void setColor(String color) {
                this.color = color;
            }

            /**
             * Is forwards boolean.
             *
             * @return the boolean
             */
            public boolean isForwards() {
                return forwards;
            }

            /**
             * Sets forwards.
             *
             * @param forwards the forwards
             */
            public void setForwards(boolean forwards) {
                this.forwards = forwards;
            }
        }

        /**
         * The type Snippet iterator.
         */
        class SnippetIterator implements Iterator<JSONObject> {
            private final int totalLines;
            private int currentLine;

            /**
             * Instantiates a new Snippet iterator.
             */
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
