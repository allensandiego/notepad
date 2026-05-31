package com.allensandiego.notepad

import com.allensandiego.notepad.db.FieldEntity
import com.allensandiego.notepad.db.TableEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class SchemaTest {

    // ---------------------------------------------------------------------------
    // Existing default resolution tests (unchanged behaviour)
    // ---------------------------------------------------------------------------

    @Test
    fun testStaticDefaultResolution() {
        val defaultValue = "Dr. Smith"
        val resolved = resolveDefault("STATIC", defaultValue)
        assertEquals("Dr. Smith", resolved)
    }

    @Test
    fun testTodayDefaultResolution() {
        val resolved = resolveDefault("TODAY", null)
        val expectedFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        assertEquals(expectedFormat, resolved)
    }

    @Test
    fun testUuidDefaultResolution() {
        val resolved = resolveDefault("UUID", null)
        assertNotNull(resolved)
        val uuid = UUID.fromString(resolved)
        assertEquals(36, uuid.toString().length)
    }

    @Test
    fun testRequiredFieldsValidation() {
        val requiredFields = listOf(
            MockField("1", "Doctor", required = true, isSystem = false),
            MockField("2", "Date", required = false, isSystem = false)
        )
        
        val invalidValues = mapOf("2" to "2026-05-30")
        val isInvalid = validateFields(requiredFields, invalidValues)
        assertTrue(!isInvalid)

        val validValues = mapOf("1" to "Dr. House", "2" to "2026-05-30")
        val isValid = validateFields(requiredFields, validValues)
        assertTrue(isValid)
    }

    // ---------------------------------------------------------------------------
    // NEW: System field (PK) auto-generation logic
    // ---------------------------------------------------------------------------

    @Test
    fun testPrimaryKeyFieldIsFlaggedAsSystem() {
        val pkField = buildSystemPkField(tableId = "table-001")
        assertTrue("PK field must be isSystem=true", pkField.isSystem)
        assertEquals("id", pkField.name)
        assertEquals("TEXT", pkField.type)
        assertEquals("UUID", pkField.defaultType)
        assertTrue("PK field must be required", pkField.required)
    }

    @Test
    fun testPrimaryKeyFieldHasValidUuid() {
        val pkField = buildSystemPkField(tableId = "table-001")
        // The field's own ID should be a valid UUID
        val uuid = UUID.fromString(pkField.id)
        assertEquals(36, uuid.toString().length)
    }

    // ---------------------------------------------------------------------------
    // NEW: FK field auto-generation for child collections
    // ---------------------------------------------------------------------------

    @Test
    fun testForeignKeyFieldNameDerivedFromParentName() {
        val parentTable = MockTable(id = "parent-001", name = "Patients", parentTableId = null)
        val childTable  = MockTable(id = "child-001",  name = "Visits",   parentTableId = "parent-001")
        val fkField = buildSystemFkField(child = childTable, parent = parentTable)
        assertEquals("Patients_id", fkField.name)
    }

    @Test
    fun testForeignKeyFieldIsFlaggedAsSystem() {
        val parentTable = MockTable(id = "parent-001", name = "Patients", parentTableId = null)
        val childTable  = MockTable(id = "child-001",  name = "Visits",   parentTableId = "parent-001")
        val fkField = buildSystemFkField(child = childTable, parent = parentTable)
        assertTrue("FK field must be isSystem=true", fkField.isSystem)
        assertTrue("FK field must be required", fkField.required)
        assertEquals("TEXT", fkField.type)
        assertNull(fkField.defaultType)
    }

    @Test
    fun testTopLevelCollectionHasNoPkOnlyParentId() {
        // Top-level collections: parentTableId == null → no FK should be generated
        val table = MockTable(id = "top-001", name = "Clients", parentTableId = null)
        assertNull("Top-level table must have null parentTableId", table.parentTableId)
    }

    // ---------------------------------------------------------------------------
    // NEW: Validation skips PK system field (it is always auto-set)
    // ---------------------------------------------------------------------------

    @Test
    fun testValidationSkipsSystemPkField() {
        // PK field is required=true but isSystem=true → validator should ignore it
        val fields = listOf(
            MockField("pk-field-id", "id", required = true,  isSystem = true),
            MockField("name-field",  "Name", required = true, isSystem = false)
        )
        // Provide only the user field; PK absent → still valid
        val values = mapOf("name-field" to "Alice")
        assertTrue("Validation must pass when only PK field value is missing", validateFieldsWithSystemSkip(fields, values))
    }

    @Test
    fun testValidationFailsWhenUserRequiredFieldMissing() {
        val fields = listOf(
            MockField("pk-field-id", "id",   required = true, isSystem = true),
            MockField("name-field",  "Name",  required = true, isSystem = false)
        )
        // Missing required non-system field
        val values = emptyMap<String, String>()
        assertFalse("Validation must fail when required user field is missing", validateFieldsWithSystemSkip(fields, values))
    }

    // ---------------------------------------------------------------------------
    // NEW: buildTreeOrder tests
    // ---------------------------------------------------------------------------

    @Test
    fun testBuildTreeOrderRootsFirst() {
        val parent = makeFakeTableEntity("p1", "Animals", null)
        val child  = makeFakeTableEntity("c1", "Dogs", "p1")
        val tables = listOf(child, parent) // intentionally out of order
        val result = buildTreeOrder(tables)
        assertEquals("Animals", result[0].first.name)
        assertEquals(0, result[0].second)
        assertEquals("Dogs", result[1].first.name)
        assertEquals(1, result[1].second)
    }

    @Test
    fun testBuildTreeOrderMultipleRoots() {
        val a = makeFakeTableEntity("a", "Alpha", null)
        val b = makeFakeTableEntity("b", "Beta",  null)
        val result = buildTreeOrder(listOf(b, a))
        // Should be alphabetically sorted at root level
        assertEquals("Alpha", result[0].first.name)
        assertEquals("Beta",  result[1].first.name)
    }

    @Test
    fun testBuildTreeOrderEmptyList() {
        val result = buildTreeOrder(emptyList<TableEntity>())
        assertTrue(result.isEmpty())
    }

    // ---------------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------------

    private fun resolveDefault(type: String?, staticValue: String?): String {
        return when (type) {
            "STATIC" -> staticValue ?: ""
            "TODAY" -> SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            "UUID" -> UUID.randomUUID().toString()
            else -> ""
        }
    }

    private fun validateFields(fields: List<MockField>, values: Map<String, String>): Boolean {
        return fields.all { field ->
            !field.required || !values[field.id].isNullOrBlank()
        }
    }

    /** Validation variant that skips system PK fields (mirrors RecordEditDialog behaviour). */
    private fun validateFieldsWithSystemSkip(fields: List<MockField>, values: Map<String, String>): Boolean {
        return fields.all { field ->
            if (field.isSystem && field.name == "id") return@all true // PK always auto-set
            !field.required || !values[field.id].isNullOrBlank()
        }
    }

    private fun buildSystemPkField(tableId: String) = FieldEntity(
        id = UUID.randomUUID().toString(),
        tableId = tableId,
        name = "id",
        type = "TEXT",
        required = true,
        defaultValue = null,
        defaultType = "UUID",
        isSystem = true
    )

    private fun buildSystemFkField(child: MockTable, parent: MockTable) = FieldEntity(
        id = UUID.randomUUID().toString(),
        tableId = child.id,
        name = "${parent.name}_id",
        type = "TEXT",
        required = true,
        defaultValue = null,
        defaultType = null,
        isSystem = true
    )

    /** Uses real TableEntity for the buildTreeOrder UI function test. */
    private fun buildTreeOrder(tables: List<TableEntity>): List<Pair<TableEntity, Int>> {
        return com.allensandiego.notepad.ui.buildTreeOrder(tables)
    }

    private fun makeFakeTableEntity(id: String, name: String, parentTableId: String?): TableEntity =
        TableEntity(id = id, name = name, parentTableId = parentTableId)

    data class MockField(val id: String, val name: String, val required: Boolean, val isSystem: Boolean)
    data class MockTable(val id: String, val name: String, val parentTableId: String?)
}
