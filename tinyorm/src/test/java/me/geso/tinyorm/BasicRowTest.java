package me.geso.tinyorm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.sql.SQLException;
import java.util.Collections;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import me.geso.jdbcutils.RichSQLException;
import me.geso.tinyorm.annotations.BeforeInsert;
import me.geso.tinyorm.annotations.BeforeUpdate;
import me.geso.tinyorm.annotations.Column;
import me.geso.tinyorm.annotations.PrimaryKey;
import me.geso.tinyorm.annotations.Table;

public class BasicRowTest extends TestBase {

	@Before
	public void before() throws SQLException {
		createTable("x",
			"id INT UNSIGNED NOT NULL PRIMARY KEY AUTO_INCREMENT",
			"name VARCHAR(255) NOT NULL",
			"y VARCHAR(255) NOT NULL");
		orm.updateBySQL("DROP TABLE IF EXISTS y");
		orm.updateBySQL("CREATE TABLE y LIKE x");

	}

	@Test
	public void testCreateUpdateStatement() throws SQLException,
			RichSQLException {
		X x = orm.insert(X.class)
			.value("name", "John")
			.executeSelect();
		assertEquals(x.getName(), "John");
		assertEquals(x.getId(), 1);
		assertEquals("inserted", x.getY());

		orm.createUpdateStatement(x)
			.set("name", "Taro")
			.execute();
		X refetched = orm.refetch(x).get();
		assertEquals("Taro", refetched.getName());
		assertEquals(1, refetched.getId());
		assertEquals("updated", refetched.getY());
	}

	@Test
	public void testRefetchWithSpecifiedConnection() throws Exception {
		X x = orm.insert(X.class).value("name", "John").executeSelect();
		assertEquals(x.getName(), "John");
		assertEquals(x.getId(), 1);
		assertEquals("inserted", x.getY());

		orm.createUpdateStatement(x).set("name", "Taro").execute();
		X refetched = orm.refetch(x, orm.getConnection()).get();
		assertEquals("Taro", refetched.getName());
		assertEquals(1, refetched.getId());
		assertEquals("updated", refetched.getY());
	}

	@Test
	public void testCreateUpdateStatementWithInheritance() throws SQLException,
			RichSQLException {
		Y y = orm.insert(Y.class)
			.value("name", "John")
			.executeSelect();
		assertEquals(y.getName(), "John");
		assertEquals(y.getId(), 1);
		assertEquals("inserted", y.getY());

		orm.createUpdateStatement(y)
			.set("name", "Jiro")
			.execute();
		Y refetched = orm.refetch(y).get();
		assertEquals("Jiro", refetched.getName());
		assertEquals(1, refetched.getId());
		assertEquals("updated", refetched.getY());
	}

	// @Test
	// public void testWhere() {
	// thrownLike(() -> {
	// X x = new X();
	// x.setConnection(connection);
	// x.setTableMeta(orm.getSchema().getTableMeta(X.class));
	// Query where = x.where();
	// System.out.println(where);
	// }, "Primary key should not be zero");
	// }

	@Test
	public void testRefetch() throws SQLException, RichSQLException {
		X x = orm.insert(X.class)
			.value("name", "John")
			.executeSelect();
		Optional<X> refetched = orm.refetch(x);
		assertEquals(refetched.get().getName(), "John");
	}

	@Test
	public void testDelete() throws SQLException, RichSQLException {
		X taro = orm.insert(X.class).value("name", "Taro")
			.executeSelect();

		X john = orm.insert(X.class).value("name", "John")
			.executeSelect();
		orm.delete(john);
		long count = orm.queryForLong("SELECT COUNT(*) FROM x", Collections.emptyList()).getAsLong();
		assertEquals(1, count);
		assertTrue(orm.refetch(taro).isPresent());
		assertFalse(orm.refetch(john).isPresent());
	}

	@Test
	public void testUpdateByBean() throws RichSQLException {
		X taro = orm.insert(X.class)
			.value("name", "Taro")
			.executeSelect();
		X member = orm.insert(X.class)
			.value("name", "John")
			.executeSelect();
		XForm xform = new XForm();
		xform.setName("Nick");
		member.update()
			.setBean(xform)
			.execute();
		assertEquals("Taro", orm.refetch(taro).get().getName()); // not
																	// modified.
		assertEquals("Nick", orm.refetch(member).get().getName());
	}

	@Getter
	@Setter
	@Table("x")
	public static class X extends Row<X> {
		@PrimaryKey
		long id;
		@Column
		String name;
		@Column
		String y;

		@BeforeInsert
		public static void fillYBeforeInsert(InsertStatement<X> stmt) {
			stmt.value("y", "inserted");
		}

		@BeforeUpdate
		public static void fillYBeforeUpdate(UpdateRowStatement<X> stmt) {
			stmt.set("y", "updated");
		}
	}

	@Data
	private static class XForm {
		String name;
	}

	@Table("y")
	public static class Y extends X {
	}

}
