package jason.storyteller;

import java.util.ArrayList;

public class SnippetFilter {
    private boolean anyOrAll;
    private ArrayList<String> includedTags = new ArrayList<>();
    private ArrayList<String> excludedTags = new ArrayList<>();

    public SnippetFilter(){
    }

    public SnippetFilter(boolean anyOrAll, ArrayList<String> includedTags, ArrayList<String> excludedTags){
        this.anyOrAll = anyOrAll;
        this.includedTags = includedTags;
        this.excludedTags = excludedTags;
    }

    public boolean isAny() {
        return anyOrAll;
    }

    public boolean isAll() {
        return !anyOrAll;
    }

    public String getAnyAll(){
        return anyOrAll ? "any" : "all";
    }

    public void setAnyOrAll(boolean anyOrAll) {
        this.anyOrAll = anyOrAll;
    }

    public ArrayList<String> getIncludedTags() {
        return includedTags;
    }

    public void setIncludedTags(ArrayList<String> includedTags) {
        for (String tag:includedTags) {
            this.includedTags.add(tag.trim());
        }
    }

    public ArrayList<String> getExcludedTags() {
        return excludedTags;
    }

    public void setExcludedTags(ArrayList<String> excludedTags) {
        for (String tag:excludedTags) {
            this.excludedTags.add(tag.trim());
        }
    }

    public boolean hasExcludedTags() {
        return excludedTags.size() > 0;
    }
}
