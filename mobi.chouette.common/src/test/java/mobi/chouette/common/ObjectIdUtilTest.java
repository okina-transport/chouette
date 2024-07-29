package mobi.chouette.common;

import org.testng.Assert;
import org.testng.annotations.Test;

@Test
public class ObjectIdUtilTest {

	public void test_composeObjectId__when_is_split_on_dot_is_true__then_generate_id_correctly() {
		String id = ObjectIdUtil.composeObjectId(true, "PRE", "VehicleJourney", "ABC.123");
		Assert.assertEquals(id, "ABC:VehicleJourney:123", "id should be generated correctly");
	}

	public void  test_composeObjectId__when_is_split_on_dot_is_false__then_generate_id_correctly() {
		String id = ObjectIdUtil.composeObjectId(false, "PRE", "VehicleJourney", "ABC.TestType.123");
		Assert.assertEquals(id, "PRE:VehicleJourney:ABC.TestType.123", "id should be generated correctly");
	}

	public void test_toGtfsId__when_keep_original_id_and_prefix_empty__then_return_neptune_id() {
		String neptuneId = "prefix:part1:part2";
		String prefix = "";
		boolean keepOriginalId = true;
		Assert.assertEquals(ObjectIdUtil.toGtfsId(neptuneId, prefix, keepOriginalId), neptuneId);
	}

	public void test_toGtfsId__when_keep_original_id_and_prefix_not_empty__then_return_prefixed_id() {
		String neptuneId = "prefix:part1:part2";
		String prefix = "newPrefix";
		boolean keepOriginalId = true;
		String expected = "newPrefix:part1:part2";
		Assert.assertEquals(ObjectIdUtil.toGtfsId(neptuneId, prefix, keepOriginalId), expected);
	}

	public void test_toGtfsId__when_not_keep_original_id_and_tokens_length_is_one__then_return_first_token() {
		String neptuneId = "part1";
		String prefix = "newPrefix";
		boolean keepOriginalId = false;
		String expected = "part1";
		Assert.assertEquals(ObjectIdUtil.toGtfsId(neptuneId, prefix, keepOriginalId), expected);
	}

	public void test_toGtfsId__when_not_keep_original_id_and_tokens_length_is_more_than_one__then_return_last_token() {
		String neptuneId = "prefix:part1:part2";
		String prefix = "newPrefix";
		boolean keepOriginalId = false;
		String expected = "part2";
		Assert.assertEquals(ObjectIdUtil.toGtfsId(neptuneId, prefix, keepOriginalId), expected);
	}

}
