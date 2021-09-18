package orm.tables

import org.ktorm.schema.Table
import org.ktorm.schema.int
import org.ktorm.schema.text
import orm.entities.RecordWarehouseType

object RecordWarehouseTypes: Table<RecordWarehouseType>("record_warehouse_types") {
    val id = int("id").primaryKey().bindTo { it.id }
    val name = text("name").bindTo { it.name }
    val description = text("description").bindTo { it.description }
}