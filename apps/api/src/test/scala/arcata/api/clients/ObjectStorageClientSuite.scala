package arcata.api.clients

import munit.FunSuite

class ObjectStorageClientSuite extends FunSuite:

  test("InMemoryObjectStorageClient should store and retrieve content"):
    val client = ObjectStorageClient.inMemory()

    val storeResult = client.store("<html>test</html>", "text/html")

    storeResult match
      case StorageResult.Success(objectId) =>
        val retrieveResult = client.retrieve(objectId)
        retrieveResult match
          case StorageResult.Success(content) =>
            assertEquals(content, "<html>test</html>")
          case StorageResult.Failure(msg, _) =>
            fail(s"Expected success, got failure: $msg")
      case StorageResult.Failure(msg, _) =>
        fail(s"Expected success, got failure: $msg")

  test("InMemoryObjectStorageClient should include prefix in object ID"):
    val client = ObjectStorageClient.inMemory()

    val storeResult = client.store("content", "text/html", "my-prefix")

    storeResult match
      case StorageResult.Success(objectId) =>
        assert(objectId.startsWith("my-prefix/"))
      case StorageResult.Failure(msg, _) =>
        fail(s"Expected success, got failure: $msg")

  test("InMemoryObjectStorageClient should delete objects"):
    val client = ObjectStorageClient.inMemory()

    val storeResult = client.store("content", "text/html")

    storeResult match
      case StorageResult.Success(objectId) =>
        val deleteResult = client.delete(objectId)
        assert(deleteResult.isInstanceOf[StorageResult.Success[?]])

        val retrieveResult = client.retrieve(objectId)
        assert(retrieveResult.isInstanceOf[StorageResult.Failure])
      case StorageResult.Failure(msg, _) =>
        fail(s"Expected success, got failure: $msg")

  test("InMemoryObjectStorageClient.retrieve should fail for non-existent objects"):
    val client = ObjectStorageClient.inMemory()

    val result = client.retrieve("non-existent-id")

    assert(result.isInstanceOf[StorageResult.Failure])

  test("InMemoryObjectStorageClient.clear should remove all objects"):
    val client = ObjectStorageClient.inMemory()

    client.store("content1", "text/html")
    client.store("content2", "text/html")
    assertEquals(client.size, 2)

    client.clear()
    assertEquals(client.size, 0)
