import java.sql.SQLException

int count = sql.executeUpdate('INSERT INTO Person (id, firstName, lastName, age) VALUES (:id, :firstName, :lastName, :age)',
  [id: 100, firstName: 'Toto', lastName: 'Chez-les-Papoos', age: 32])
if (count != 1) {
  println 'hum ... an unexpected error occurred!'
  throw new SQLException('INSERTION FAILURE!')
} else {
  println 'Ho yeh, I succeeded to add my person into the database!'
}