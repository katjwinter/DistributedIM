import java.rmi.*;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Set;

/**
 * Implementation of the iNode interface for a Node in a DHT
 * @author Kat Winter
 */
public class Node implements iNode {
	
	private iNode nextNode;
	private iNode prevNode;
	private String nodeIP;
	private iNode self;
	private String identifier;
	private Hashtable<String, Object> storage;
	private boolean bootStrap = false;
	
	/**
	 * Constructor for a new Node in the DHT
	 * @param nodeIP unhashed IP address of the Node
	 */
	public Node(String nodeIP) {
		
		this.nodeIP = nodeIP;
		identifier = Hash.hash(nodeIP);
		storage = new Hashtable<String, Object>();
		
		try {
			self = (iNode) UnicastRemoteObject.exportObject(this, 0);
		} catch (RemoteException e) {
			System.out.println("Error creating remote object of myself");
			e.printStackTrace();
		}
		System.out.println("Identifier for " + nodeIP + " is " + identifier);
		
		nextNode = self;
		prevNode = self;
	}
	
	/**
	 * Set up a bootstrap for the Ring (create registry on 1099)
	 * @throws RemoteException
	 */
	public void beBootStrap() throws RemoteException {
		
		if (!bootStrap) {
		
			Registry reg = LocateRegistry.createRegistry(1099);
			reg.rebind("bootstrap", self);
			bootStrap = true;
		}
	}
	
	/**
	 * Create a Ring as its first member
	 */
	public void create() throws RemoteException {
		
		beBootStrap();
	}
	
	/**
	 * Open a socket to an existing Node (bootstrap) and register this
	 * Node as part of the ring
	 * @param bootIP the IP of the existing Node acting as bootstrap
	 * @throws RemoteException, NoBoundException
	 */
	public void join(String bootIP) throws RemoteException, NotBoundException {
			
		Registry reg = LocateRegistry.getRegistry(bootIP);
		iNode bootStrap = (iNode) reg.lookup("bootstrap");
		bootStrap.addNodeToRing(self);
	}
	
	/**
	 * Transfer data to a Node that has just joined the Ring
	 * @param Node to receive the key/data values
	 */
	public void transferData(iNode newNode) {
		
		Hashtable<String, Object> storageCopy = new Hashtable<String, Object>();
		
		if (!storage.isEmpty()) {
			Set<String> set = storage.keySet();
			String key;

			Iterator<String> itr = set.iterator();
			while (itr.hasNext()) {
				key = itr.next();
				// If the key of this object is less than that of the new node, have new node store it instead
				try {

					if (key.compareTo(newNode.getID()) < 0) { 
						newNode.insert(key, storage.get(key));
					}

					// Else key,val pair should stay with this node
					else { 
						storageCopy.put(key, storage.get(key));
					}
				} catch (RemoteException e) {
					System.out.println("Problem inserting keys to new node");
				}
			}
			storage = storageCopy;
		}
	}
	
	/**
	 * Transfer all keys/data from this Node to other Node
	 * @param Node to receive the data
	 */
	private void fullTransfer(iNode recipient) {
		
		if (!storage.isEmpty()) {
			Set<String> set = storage.keySet();
			String key;

			Iterator<String> itr = set.iterator();
			while (itr.hasNext()) {
				key = itr.next();
				try {
					recipient.insert(key, storage.get(key));
				} catch (RemoteException e) {
					System.out.println("Problem inserting keys to recipient node");
				}
			}

			storage.clear();
		}
	}
	
	/**
	 * Insert key/data directly into this node's storage
	 * Precondition: This key,val pair should belong to this node
	 * @param key Key for the hashtable
	 * @param data Value for the hashtable
	 */
	public void insert(String key, Object data) {
		
		storage.put(key, data);
	}
	
	public void addNodeToRing(iNode newNode) {
		
		try {
			
			// Check if the new node should be inserted after this node
			if (newNode.getID().compareTo(identifier) > 0) {
				
				// Check if this node is actually the last node (or only node) 
				// Or that the new node is smaller than the next node, in which case 
				// the new node should be inserted immediately after this one
				// Have the next node transfer appropriate data over to the new node
				if (identifier.compareTo(nextNode.getID()) >= 0 || newNode.getID().compareTo(nextNode.getID()) < 0) {
					
					nextNode.setPrev(newNode);
					newNode.setNext(nextNode);
					newNode.setPrev(self);
					
					nextNode.transferData(newNode);
					
					setNext(newNode);
				}
				
				// Otherwise send it on to the next node to handle adding
				else {
					nextNode.addNodeToRing(newNode);
				}
			}
			
			// Otherwise the new node should come prior to this node
			else {
				
				// Check if this node is actually the first node (or only node) 
				// Or that the new node is larger than the previous node, in which case
				// the new node should be inserted immediately prior to this one
				// So this node needs to transfer appropriate data over to the new node
				if (identifier.compareTo(prevNode.getID()) <= 0 || newNode.getID().compareTo(prevNode.getID()) > 0) {
					
					prevNode.setNext(newNode);
					newNode.setPrev(prevNode);
					newNode.setNext(self);
					setPrev(newNode);
					
					transferData(newNode);
				}
				
				// Otherwise send it on to the previous node to handle adding
				else {
					
					prevNode.addNodeToRing(newNode);
				}
			}
			
		} catch (RemoteException e) {
			System.out.println("Remote Exception");
			e.printStackTrace();
		}		
	}
	
	/**
	 * Assign the Node following this Node
	 */
	public void setNext(iNode next) {
		nextNode = next;
	}
	
	/**
	 * Assign the Node prior to this Node
	 */
	public void setPrev(iNode prev) {
		prevNode = prev;
	}

	/**
	 * @return Node following this Node
	 */
	public iNode getNext() {
		return nextNode;
	}

	/**
	 * @return Node preceding this Node
	 */
	public iNode getPrev() {
		return prevNode;
	}

	/**
	 * @return hashed IP address for this node
	 */
	public String getID() {
		return identifier;
	}
	
	/**
	 * @return unhashed IP address for this node
	 */
	private String getIP() {
		return nodeIP;
	}

	/**
	 * Transfer all hash table data to the next node
	 * unless this node was the largest in the ring, in which case
	 * transfer all hash table data to the previous node
	 */
	public void leave() {
		
		try {
			
			// If this node is the largest in the ring, send all hash table data to previous node
			if (identifier.compareTo(nextNode.getID()) > 0) {
				fullTransfer(prevNode);
				
				// Effectively remove this node from the Ring
				nextNode.setPrev(prevNode);
				prevNode.setNext(nextNode);
			}
			
			// if this node is not the largest in the ring, send all hash table data to the next node
			else if (identifier.compareTo(nextNode.getID()) < 0) {
				fullTransfer(nextNode);
				
				// Effectively remove this node from the Ring
				nextNode.setPrev(prevNode);
				prevNode.setNext(nextNode);
			}
			
		} catch (RemoteException e) {
			System.out.println("For logging purposes: Problem leaving Ring");
		}
	}
	
	/**
	 * Check if a key already exists in the DHT
	 * @param key Unhashed key to search for
	 * @return True if the key already exists, False if it does not
	 * @throws RemoteException
	 */
	public boolean checkKeyExists(String key) throws RemoteException {
		
		Object test;

			test = get(key);
		
		if (test != null) {
			return true;
		}
		
		return false;
	}

	/**
	 * Return the requested data item based on hashing the key k.
	 * @param k Unhashed key corresponding to the value/data that requester needs
	 * @return The value/data corresponding to the Key k
	 * @throws RemoteException
	 */
	public Object get(String k) throws RemoteException {

		String key = Hash.hash(k);

		// If the key is bigger than the identifier for this node, check the next node
		if (key.compareTo(identifier) > 0) {

			String nextIdentifier = nextNode.getID();

			// If this node is the biggest node in the Ring, or the only node, then I have the data
			if (identifier.compareTo(nextIdentifier) >= 0) {
				return storage.get(key);
			}

			// The key is bigger than me and I'm not the biggest node, so need to get it from the next node after me
			else {
				return nextNode.get(k);
			}
		}

		// The key is smaller than this node
		else {

			String prevIdentifier = prevNode.getID();

			// If this node is the smallest node in the ring, or this is the only node, I should have the data
			if (identifier.compareTo(prevIdentifier) <= 0) {
				return storage.get(key);
			}

			// If the key is smaller than the previous node, get it from the previous node
			else if (prevIdentifier.compareTo(key) > 0) {
				return prevNode.get(k);
			}

			// Otherwise the key is smaller than this node but larger than the previous node, which means I should have it
			else {
				return storage.get(key);
			}
		}
	}

	/**
	 * Store value/data matching a hash of the given Key k
	 * @param k Unhashed key corresponding to the given data
	 * @param data The data to be stored at this node according to key K
	 * @throws RemoteException
	 */
	public void put(String k, Object data) throws RemoteException {
		
		String key = Hash.hash(k);

		// If the key is bigger than the identifier for this node, check the next node
		if (key.compareTo(identifier) > 0) {

			String nextIdentifier = nextNode.getID();

			// If this node is the biggest node in the Ring, or I am the only node, then I should store the data
			if (identifier.compareTo(nextIdentifier) >= 0) {
				storage.put(key, data);
			}

			// The key is bigger than me and I'm not the biggest node, so need to send it over to the next node after me
			else {
				nextNode.put(k, data);
			}
		}
		
		// If the key is smaller than the identifier for this node, check to see if it's bigger than the previous node
		else {

			String prevIdentifier = prevNode.getID();

			// If this node is the smallest node in the ring, or this is the only node, I should store the data
			if (identifier.compareTo(prevIdentifier) <= 0) {
				storage.put(key, data);
			}

			// If the key is smaller than the previous node, send to that node
			else if (prevIdentifier.compareTo(key) > 0) {
				prevNode.put(k, data);
			}

			// Otherwise I should store the data
			else {
				storage.put(key, data);
			}
		}	
	}
	
	/**
	 * Removes a key/value pair from the DHT
	 * @param k the unhashed key for removal
	 */
	public void remove(String k) {

		String key = Hash.hash(k);

		try {
			// If the key is bigger than the identifier for this node, check the next node
			if (key.compareTo(identifier) > 0) {

				String nextIdentifier = nextNode.getID();

				// If this node is the biggest node in the Ring, or I am the only node, then I have the data to be removed
				if (identifier.compareTo(nextIdentifier) >= 0) {
					
					storage.remove(key);
				}

				// The key is bigger than me and I'm not the biggest node, so need to send it over to the next node after me
				else {
					nextNode.remove(k);
				}
			}

			// If the key is smaller than the identifier for this node, check to see if it's bigger than the previous node
			else {

				String prevIdentifier = prevNode.getID();

				// If this node is the smallest node in the ring, or this is the only node, I have the data to be removed
				if (identifier.compareTo(prevIdentifier) <= 0) {
					
					storage.remove(key);
				}

				// If the key is smaller than the previous node, send to that node
				else if (prevIdentifier.compareTo(key) > 0) {
					
					prevNode.remove(k);
				}

				// Otherwise I have the data to be removed
				else {
					
					storage.remove(key);
				}
			}
		} catch (RemoteException e) {
			System.out.println("For logging purposes: Failed to remove key/data from DHT");
		}
	}
}
