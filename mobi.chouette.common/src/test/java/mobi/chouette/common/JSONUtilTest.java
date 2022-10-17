package mobi.chouette.common;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

public class JSONUtilTest {

	@Test(groups = { "JSONUtil" }, description = "string conversion")
	public void testString() throws Exception {
		JsonModel model = new JsonModel();
		model.setName("theName");
		model.setUserName("theUserName");
		model.setValue(Integer.valueOf(12));
		model.getList().add("test");
		JsonChildModel childModel = new JsonChildModel();
		childModel.setData("myData");
		childModel.getNames().add("myName");
		model.setChild(childModel);
		
		String json = JSONUtil.toJSON(model);
		assertEqualsIgnoreSpace(json, "{\"json_model\": {\n" +
				"  \"name\": \"theName\",\n" +
				"  \"user_name\": \"theUserName\",\n" +
				"  \"value\": 12,\n" +
				"  \"list\": [\"test\"],\n" +
				"  \"child\": {\n" +
				"    \"data\": \"myData\",\n" +
				"    \"names\": [\"myName\"]\n" +
				"  }\n" +
				"}}", "json string");

		JsonModel model2 = JSONUtil.fromJSON(json, JsonModel.class);
		Assert.assertEquals(model2.getName(),model.getName(),"name");
		Assert.assertEquals(model2.getUserName(),model.getUserName(),"user name");
		Assert.assertEquals(model2.getValue(),model.getValue(),"value");
	}

	
	@Test(groups = { "JSONUtil" }, description = "file conversion")
	public void testFile() throws Exception {

		JsonModel model = new JsonModel();
		model.setName("theName");
		model.setUserName("theUserName");
		model.setValue(Integer.valueOf(12));
		
		Path path = Paths.get("jsonModel.json");
		File f = new File(path.toString());
		
		JSONUtil.toJSON(path, model);
		String json = FileUtils.readFileToString(f);
		assertEqualsIgnoreSpace(json, "{\"json_model\": {\n" +
				"  \"name\": \"theName\",\n" +
				"  \"user_name\": \"theUserName\",\n" +
				"  \"value\": 12\n" +
				"}}", "json string");

		JsonModel model2 = JSONUtil.fromJSON(path, JsonModel.class);
		Assert.assertEquals(model2.getName(),model.getName(),"name");
		Assert.assertEquals(model2.getUserName(),model.getUserName(),"user name");
		Assert.assertEquals(model2.getValue(),model.getValue(),"value");
		
		f.delete();
	}

	private static void assertEqualsIgnoreSpace(String json, String expected, String json_string) {
		Assert.assertEquals(StringUtils.deleteWhitespace(json), StringUtils.deleteWhitespace(expected), json_string);
	}

}
