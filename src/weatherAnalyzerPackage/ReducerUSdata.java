package weatherAnalyzerPackage;

import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import java.io.IOException;

public class ReducerUSdata extends Reducer<Text, Text, Text, Text> {
  private Text newKey = new Text();
  private Text usData = new Text();

  @SuppressWarnings({ "unchecked" })
  @Override
  protected void reduce(Text key, Iterable<Text> values, Context context) throws IOException, InterruptedException {

    int recordMonth = 0;
    int countTemp = 0;
    int countPrcp = 0;
    float runningTemp = (float) 0.0;
    float runningPrcp = (float) 0.0;
    float avgTemp = (float) 0.0;
    float avgPrcp = (float) 0.0;
    String state = ""; // get state from values
    String month = ""; // get month from values
    
    for (Text value : values) {
      
      // Parse into JSON Data
      Object objJSON = JSONValue.parse(value.toString());
      JSONArray jsonData=(JSONArray)objJSON;
      JSONObject obj=(JSONObject)jsonData.get(0);
      
      // Retrieve Values
      state = (String) obj.get("STATE");
      if (state.isEmpty() || state.equals("STATE")) {
        state = "XX";
      } 
      
      month = (String) obj.get("YEARMODA");
      if (!month.isEmpty()) {
        month = month.substring(4,6);
        recordMonth = Integer.parseInt(month);
      } else {
        month = "00";
      }
      
      newKey.set(state + "-" + month);
      
      // Get temps
      String tempString = (String) obj.get("TEMP");
      if (tempString != null && !tempString.isEmpty()) {
        float temp = Float.parseFloat( (String) obj.get("TEMP"));
        runningTemp = runningTemp + temp; // add up all values for the month
        countTemp++;
      }   
      
      /* Get PRCP, DEAL WITH LETTERS
       * 
       *  A - 6 hours worth of precipitation
       *    B - 12 hours...
       *    C - 18 hours...
       *    D - 24 hours...
       *    E - 12 hours... (slightly different from B but the same for this project).
       *    F - 24 hours ... (slightly different from D but the same for this project).
       *    G - 24 hours ... (slightly different from D but the same for this project).
       *    H - station recorded a 0 for the day (although there was some recorded instance of precipitation).
       *    I - station recorded a 0 for the day (and there was NO recorded instance of precipitation).
       */
      String prcpString = (String) obj.get("PRCP");
      if (prcpString != null && !prcpString.isEmpty()) {
        
        String flagPrcp = prcpString.substring((prcpString.length() - 1), prcpString.length());
        String tempPrcp = prcpString.substring(0,(prcpString.length() - 2));
        
        float prcp = Float.parseFloat( tempPrcp);
        switch(flagPrcp) {
          case "A":
            prcp = prcp * 4;
            break;
            
          case "B":
            prcp = prcp * 2;
            break;
            
          case "C":
            prcp = prcp * (float)1.33;
            break;
            
          case "D":
            prcp = prcp * 1;
            break;
  
          case "E":
            prcp = prcp * 2;
            break;
  
          case "F":
            prcp = prcp * 1;
            break;
            
          case "G":
            prcp = prcp * 1;
            break;
            
          case "H":
            prcp = prcp * 1;
            break;
            
          case "I":
            prcp = (float) 9999.0;
            break;
          
        }
        
        if (prcp < 9000) {
          runningPrcp = runningPrcp + prcp; // add up all values for the month
          countPrcp++;
        }
      } 
    }
    
    // output STATE, MONTH, AVGTEMPm AVGPRCP
    avgTemp = runningTemp / countTemp; // get average temp for this month
    avgPrcp = runningPrcp / countPrcp; // get average temp for this month
    JSONObject outputData = new JSONObject();
    outputData.put("STATE", state);
    outputData.put("MONTH", recordMonth);
    outputData.put("AVGTEMP", avgTemp);
    outputData.put("AVGPRCP", avgPrcp);
    JSONArray jsonArray = new JSONArray();
    jsonArray.add(outputData);
    
    usData.set(jsonArray.toJSONString());
    context.write(newKey, usData);
  }
}
