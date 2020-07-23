package jason.storyteller;

import java.util.ArrayList;

public class SnippetFilter {
    private boolean anyOrAll;
    private ArrayList<String> includedTags;
    private ArrayList<String> excludedTags;

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

    public void setAnyOrAll(boolean anyOrAll) {
        this.anyOrAll = anyOrAll;
    }

    public ArrayList<String> getIncludedTags() {
        return includedTags;
    }

    public void setIncludedTags(ArrayList<String> includedTags) {
        this.includedTags = includedTags;
    }

    public ArrayList<String> getExcludedTags() {
        return excludedTags;
    }

    public void setExcludedTags(ArrayList<String> excludedTags) {
        this.excludedTags = excludedTags;
    }
}
