package jason.storyteller;

import org.json.JSONObject;

public class DelaySet {
    private int[] delays = new int[4];

    public DelaySet(int[] delays){
        this.delays = delays;
    }

    public DelaySet(int pre, int name, int arrow, int text){
        delays[0] = pre;
        delays[1] = name;
        delays[2] = arrow;
        delays[3] = text;
    }

    public DelaySet(int pre, int text){
        delays[0] = pre;
        delays[1] = text;
    }

    public void copy(DelaySet copying){
        delays[0] = copying.getPreDelay();
        delays[1] = copying.getNameDelay();
        delays[2] = copying.getArrowDelay();
        delays[3] = copying.getTextDelay();
    }

    public int getPreDelay(){
        return delays[0];
    }

    public int getNameDelay(){
        return delays[1];
    }

    public int getArrowDelay(){
        return delays[2];
    }

    public int getTextDelay(){
        return delays[3];
    }

    public int getDiagTextDelay(){
        return delays[1];
    }

    /**
     * Generate a new delayset based on a template with a JSONObject that may contain override values.
     *
     * @param jsonDelays    the jsonobject containing any specific overrides. it may be null.
     * @param template      the template to work from.
     */
    public DelaySet(JSONObject jsonDelays, DelaySet template) {
        // if jsonDelays is null, that means that this line didn't have any specified adjustments to make to speed.
        if (jsonDelays != null) {
            // once those conditions are clear, we check each possible speed and override the relevant speed in the given array
            delays[0] = jsonDelays.optInt("pre", template.getPreDelay());
            delays[1] = jsonDelays.optInt("name", template.getNameDelay());
            delays[2] = jsonDelays.optInt("tick", template.getArrowDelay());
            delays[3] = jsonDelays.optInt("text", template.getTextDelay());
        } else {
            this.copy(template);
        }
    }
}
