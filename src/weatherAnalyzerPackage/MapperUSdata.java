package weatherAnalyzerPackage;

import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import java.io.IOException;

public class MapperUSdata extends Mapper<LongWritable, Text, Text, Text> {
  private Text newKey = new Text();
  private Text newValues = new Text();

  /*
   * Map to STATE-MONTH
   */   
  @Override
  protected void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException {

    // Convert value to a String
    String line = value.toString();
    line = line.substring((line.indexOf(",") + 1), line.length());    
    
    if (line.length() > 0 && line.substring(0,1).equals("[") ) { // JSON string passed in
      
      /* Parse into JSON Data*/
      Object objJSON = JSONValue.parse(line);
      JSONArray jsonData=(JSONArray)objJSON;
      JSONObject obj=(JSONObject)jsonData.get(0);
      
      // Retrieve Values
      String state = (String) obj.get("STATE");
      if (state.isEmpty() ) {
        state = "XX";
      }
      
      String month = (String) obj.get("YEARMODA");
      if (!month.isEmpty()) {
        month = month.substring(4,6);
      } else {
        month = "00";
      }
            
      // Assign values and output
      newKey.set(state + "-" + month);
      newValues.set(line); 
      context.write(newKey, newValues);
    }
  }
}
