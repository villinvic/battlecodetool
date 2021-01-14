// Map polyfill
import * as Map from 'core-js/es/map';

// To somebody good at typescript, please fix @ts-ignore

/**
 * A class that wraps a group of typed buffers.
 *
 * Say you want to store a bunch of game entities. You could store them like this:
 * let entities = [
 *   {id: 0, x: 100, y: 35, size: 56},
 *   {id: 1, x: 300, y: 24, size: 73},
 *   ...
 * ];
 * However, this creates memory overhead, gc pressure, and spreads your objects
 * all through memory.
 *
 * Instead, you can store them like this:
 * let entities = {
 *   id: new Uint16Array([0, 1, ...]),
 *   x: new Float64Array([100, 300, ...]),
 *   y: new Float64Array([35, 24, ...]),
 *   size: new Float64Array([56, 73, ...]),
 * };
 *
 * This is more space-efficient, iteration is fast, and if you're working with
 * an API that takes typed array views (say, webgl), you can pass your field
 * arrays in directly.
 *
 * It is more awkward to use, though. This class makes it easier.
 * type EntitySchema = {
 *   id: Uint16Array,
 *   x: Float64Array,
 *   y: Float64Array,
 *   size: Float64Array;
 * }
 *
 * let entities = new StructOfArrays<EntitySchema>({
 *   id: new Uint16Array([0, 1, ...]),
 *   x: new Float64Array([100, 300, ...]),BodiesSchema
 *
 * Note that one field is treated as the 'primary key' (although there aren't
 * actually secondary keys), and is used to uniquely identify objects.
 *
 * Invariants:
 * All data in the arrays is stored from index 0 to index soa.length - 1.
 * Primary keys may not be repeated.
 */
export default class StructOfArrays<Schema extends ValidSchema> {
  /**
   * The actual storage.
   * You can access this, but you have to be careful not to break any
   * invariants.
   * In particular, you can't trust the length field of these TypedArrays;
   * you have to use soa.length.
   */
  readonly arrays: Schema & ValidSchema;

  /**
   * The actual length of all arrays.
   * Arrays are resized asymptotically to power-of-two lengths
   * as we resize.
   */
  private _capacity: number;

  /**
   * The logical length of the container.
   */
  private _length: number;

  /**
   * The names of our fields.
   */
  private readonly _fieldNames: Array<keyof Schema>;

  /**
   * The lookup table for our primary key. Maps keys to indices.
   * We attempt to use an ES6 Map for this, since it won't stringify
   * keys all the time.
   */
  private readonly _primLookup: Map<number, number>;

  /**
   * The name of our primary key.
   */
  private readonly _primary: keyof Schema;

  // Cache fields
  // (Not needed, just to avoid allocating)

  /**
   * An array we use to store intermediate indices generated while working.
   * May be null.
   */
  private _indexBuffer?: Uint32Array;

  /**
   * Use like:
   * const db = new StructOfArrays({
   *   id: StructOfArrays.types.int32,
   *   x: StructOfArrays.types.float64,
   *   y: StructOfArrays.types.float64
   * }, 'id')
   *
   * @param fields the names and types of fields in the SOA
   * @param primary the primary key of the SOA
   * @param capacity the initial capacity of the SOA
   */
  constructor(fields: Schema & ValidSchema, primary: keyof Schema) {
    if (!hasOwnProperty(fields, String(primary))) {
      // redundant unless somebody gets cocky
      throw new Error(`Primary key must exist, '${primary}' not found`);
    }

    this.arrays = Object.create(null);
    this._length = fields[primary].length;
    this._capacity = this._length;
    this._fieldNames = new Array<keyof Schema>();
    this._primLookup = new Map<number, number>();
    this._primary = primary;
    this._indexBuffer = undefined;

    for (const field in fields) {
      if (hasOwnProperty(fields, field)) {
        const arr = fields[field];
        // @ts-ignore
        this.arrays[field] = new (<any>arr).constructor(this._capacity);
        this.arrays[field].set(arr.slice(0, this._length));
        this._fieldNames.push(field);
      }
    }

    this._refreshPrimariesLookup(this._length)
  }

  /**
   * Create a copy of this StructOfArrays.
   * Capacity of the copy will be shrunk to this.length.
   */
  copy(): StructOfArrays<Schema> {
    return new StructOfArrays<Schema>(this.arrays, this._primary);
  }

  /**
   * Copy source's buffers into ours, overwriting all values.
   * @throws Error if source is missing any of our arrays
   */
  copyFrom<LargerSchema extends Schema>(source: StructOfArrays<LargerSchema>) {
    this._length = source.length;

    if (this._capacity < source.length) {
      this._capacity = source.length;
      for (const field in this.arrays) {
        const oldArray = this.arrays[field];
        const newArray = new (<any>oldArray.constructor)(this._capacity);
        // @ts-ignore
        this.arrays[field] = newArray;
      }
    }

    for (const field of this._fieldNames) {
      if (!(field in source.arrays)) {
        throw new Error(`Can't copyFrom, source missing field ${field}`);
      }
      this.arrays[field].set(source.arrays[field].slice(0, source.length));
      StructOfArrays.fill(<any>this.arrays[field], 0, source.length, this._capacity);
    }

    this._refreshPrimariesLookup(this._length)
  }

  /**
   * Get the length of the entries in the array.
   */
  get length(): number {
    return this._length;
  }

  /**
   * Delete everything.
   */
  clear() {
    // zero all arrays
    for (const name of this._fieldNames) {
      const array = this.arrays[name];
      StructOfArrays.fill(<any> array, 0, 0, this._capacity);
    }
    // clear key lookup
    this._primLookup.clear();
    // no elements
    this._length = 0;
  }

  /**
   * Insert a struct into the array.
   * Note: numbers with no corresponding entry will set their
   * corresponding fields to 0.
   *
   * @return index of inserted object
   */
  insert(numbers: Partial<Row<Schema>>): number {
    if (!(this._primary in numbers)) {
      throw new Error('Cannot insert without primary key');
    }
    const primary = numbers[this._primary];
    // @ts-ignore
    if (this._primLookup.has(primary)) {
      throw new Error('Primary key already exists');
    }

    this._resize(this._length + 1);

    const index = this._length - 1;
    // @ts-ignore
    this._primLookup.set(primary, index);
    this._alterAt(index, numbers);
    return index;
  }

  /**
   * Modify an existing struct in the array.
   *
   * @return index of altered object (NOT primary key)
   */
  alter(numbers: Partial<Row<Schema>>): number {
    if (!(this._primary in numbers)) {
      throw new Error(`Cannot alter without primary key: '${this._primary}'`);
    }
    // @ts-ignore
    const p = <number> (numbers[this._primary]);
    if (!this._primLookup.has(p)) {
      throw new Error(`Record with primary key does not exist: ${p}`);
    }
    const index = this._primLookup.get(p);
    this._alterAt(index, numbers);
    return index;
  }

  /**
   * Look up a primary key in the array.
   */
  lookup(primary: number, result: Partial<Row<Schema>>=Object.create(null)):
      Row<Schema> {
    if (!this._primLookup.has(primary)) {
      throw new Error(`Record with primary key does not exist: ${primary}`);
    }
    const i = this._primLookup.get(primary);
    for (const field of this._fieldNames) {
      // @ts-ignore
      result[field] = this.arrays[field][i];
    }
    return <Row<Schema>> result;
  }

  /**
   * @return the index of the object with the given primary key,
   * or -1.
   */
  index(primary: number): number {
    const index = this._primLookup.get(primary);

    return index === undefined? -1 : index;
  }

  /**
   * Set at an array index.
   */
  private _alterAt(index: number, values: Partial<Row<Schema>>) {
    for (const field in values) {
      if (hasOwnProperty(values, field) && field in this.arrays) {
        // @ts-ignore
        this.arrays[field][index] = <number> (values[field]);
      }
    }
  }

  /**
   * Delete at an array index.
   * O(this.length); prefer deleting in bulk.
   */
  delete(key: number) {
    const arr = new (<any> this.arrays[this._primary].constructor)(1);
    arr[0] = key;
    this.deleteBulk(arr);
  }

  /**
   * Insert values in bulk.
   * O(values[...].length).
   *
   * Values are guaranteed to be inserted in a single block.
   * You can perform extra initialization on this block after insertion.
   *
   * The block is in range [startI, this.length)
   *
   * @return startI
   */
  insertBulk(values: Partial<Schema & ValidSchema>): number {
    if (!hasOwnProperty(values, String(this._primary))) {
      throw new Error(`Cannot insert without primary key: '${this._primary}'`);
    }
    const primaries = <any> values[this._primary];

    const startInsert = this._length;
    this._resize(this._length + primaries.length);
    let err = false;

    for (const field in values) {
      if (hasOwnProperty(values, field) && field in this.arrays && values[field] != null) {
        this.arrays[field].set(<any> values[field], startInsert);
      }
    }
    for (let i = 0; i < primaries.length; i++) {
      this._primLookup.set(primaries[i], startInsert + i);
    }
    return startInsert;
  }

  /**
   * Alter values in bulk.
   * O(values[...].length).
   * Rows with nonexistent primary keys will be silently ignored.
   */
  alterBulk(values: Partial<Schema & ValidSchema>) {
    if (!hasOwnProperty(values, String(this._primary))) {
      throw new Error(`Cannot alter without primary key: '${this._primary}'`);
    }
    const indices = this.lookupIndices(<any> values[this._primary]);
    for (const field in values) {
      if (hasOwnProperty(values, field) && (field in this.arrays)
          && field != this._primary && values[field] != null) {
        this._alterBulkFieldImpl(<any> this.arrays[field], indices, <any> values[field]);
      }
    }
  }

  /**
   * Lookup the indices of a set of primary keys.
   * Returned array may not be the length of primaries; ignore extra entries.
   */
  lookupIndices(primaries: TypedArray): Uint32Array {
    if (this._indexBuffer == null || this._indexBuffer.length < primaries.length) {
      this._indexBuffer = new Uint32Array(StructOfArrays._capacityForLength(primaries.length));
    }
    const p = this._primLookup;
    let indexCount = 0;
    for (let i = 0; i < primaries.length; i++) {
      const key = p.get(primaries[i]);
      this._indexBuffer[i] = key === undefined? -1 : key;
    }
    return this._indexBuffer;
  }

  /**
   * Let the JIT have a small, well-typed chunk of array code to work with.
   */
  private _alterBulkFieldImpl(target: TypedArray, indices: TypedArray, source: TypedArray) {
    for (let i = 0; i < source.length; i++) {
      target[indices[i]] = source[i];
    }
  }

  /**
   * Copy a value into a TypedArray (or normal array, I suppose).
   *
   * Just a polyfill.
   *
   * @param arr the array
   * @param value the value to fill with
   * @param start inclusive
   * @param end exclusive
   */
  static fill(arr: TypedArray, value: number, start: number, end: number) {
    if (arr.fill) {
      arr.fill(value, start, end);
    } else {
      for (let i = start; i < end; i++) {
        arr[i] = value;
      }
    }
  }

  /**
   * Create a sorted array of keys to delete.
   * May allocate a new array, or reuse an old one.
   * Supplying nonexistent or repeated keys is not allowed.
   */
  private _makeToDelete(keys: TypedArray): TypedArray {
    if (this._indexBuffer == undefined || this._indexBuffer.length < keys.length) {
      this._indexBuffer = new Uint32Array(StructOfArrays._capacityForLength(keys.length));
    }
    let indexCount = 0;
    for (let i = 0; i < keys.length; i++) {
      const key = this._primLookup.get(keys[i]);
      if (key === undefined) continue;

      this._indexBuffer[indexCount] = key
      indexCount++;
    }

    let t = this._indexBuffer.subarray(0, indexCount);

    if (Uint32Array.prototype.sort) {
      // note: sort, by default, sorts lexicographically.
      // Javascript!
      t.sort(SENSIBLE_SORT);
    } else {
      Array.prototype.sort.call(t, SENSIBLE_SORT);
    }
    return t;
  }

  /**
   * Delete a set of primary keys.
   */
  deleteBulk(keys: TypedArray) {
    if (keys.length === 0) return;

    // map the keys to indices and sort them
    const toDelete = this._makeToDelete(keys);

    for (const name of this._fieldNames) {
      const array = this.arrays[name];
      // copy the fields down in the array
      this._deleteBulkFieldImpl(<Uint32Array>(toDelete), <any>array);

      // zero the new space in the array
      StructOfArrays.fill(<any> array, 0, this._length - toDelete.length, this._length);
    }
    this._length -= toDelete.length;
    this._refreshPrimariesLookup(this._length);
  }

  /**
   * @param toDelete at least one element; sorted ascending
   * @param array the array to modify
   */
  private _deleteBulkFieldImpl(toDelete: Uint32Array, array: TypedArray) {
    let length = this._length;
    let off = 1;
    for (let i = toDelete[0]+1; i < length; i++) {
      if (toDelete[off] === i) {
        off++;
      } else {
        array[i-off] = array[i];
      }
    }
  }

  /**
   * Update the indices in the lookup table
   */
  private _refreshPrimariesLookup(newLength: number) {
    const p = this.arrays[this._primary];
    this._primLookup.clear();
    for (let i = 0; i < newLength; i++) {
      this._primLookup.set(p[i], i);
    }
  }

  /**
   * Resize internal storage, if needed.
   */
  private _resize(newLength: number) {
    if (newLength > this._capacity) {
      this._capacity = StructOfArrays._capacityForLength(newLength);
      for (const field in this.arrays) {
        const oldArray = this.arrays[field];
        const newArray = new (<any> oldArray.constructor)(this._capacity);
        newArray.set(oldArray);
        // @ts-ignore
        this.arrays[field] = newArray;
      }
    }

    this._length = newLength;
  }

  /**
   * Round up to the nearest power of two.
   */
  private static _capacityForLength(size: number): number {
    // see http://graphics.stanford.edu/~seander/bithacks.html
    if (size <= 0) {
      return 0;
    }
    // size is a power of two
    if ((size & (size-1)) === 0) {
      return size;
    }
    // round up to the next power of two
    size--;
    size |= size >> 1;
    size |= size >> 2;
    size |= size >> 4;
    size |= size >> 8;
    size |= size >> 16;
    size++;
    return size
  }

  /**
   * Check invariants.
   */
  assertValid() {
    // test primary key lookup / uniqueness
    const primary = this.arrays[this._primary];
    for (let i = 0; i < this._length; i++) {
      if (this._primLookup.get(primary[i]) !== i) {
        throw new Error(`Incorrect: key '${primary[i]}', actual index ${i}, cached index ${this._primLookup.get(primary[i])}`);
      }
    }
    for (const field of this._fieldNames) {
      if (this.arrays[field].length !== this._capacity) {
        throw new Error(`Capacity mismatch: supposed to be ${this._capacity}, actual ${this.arrays[field].length}`);
      }
      for (let i = this._length; i < this._capacity; i++) {
        if (this.arrays[field][i] !== 0) {
          throw new Error(`Array not zeroed after length: ${field}`);
        }
      }
    }
  }
}

/**
 * An object corresponding to a row in the database.
 */
export type Row<Schema> = {[P in keyof Schema]: number};


/**
 * An array allocated as a contiguous block of memory.
 * Backed by an ArrayBuffer.
 */
export type TypedArray = Int8Array | Uint8Array | Int16Array | Uint16Array | Int32Array | Uint32Array | Float32Array | Float64Array;

/**
 * Valid schema types.
 * Schemas cannot contain types aside from typed arrays.
 * Amazingly, this works.
 */
export type ValidSchema = {[id: string]: TypedArray};

/**
 * In case the object has deleted hasOwnProperty.
 */
function hasOwnProperty(obj: any, prop: string) {
  return Object.prototype.hasOwnProperty.call(obj, prop);
}

/**
 * Javascript, everybody!
 */
const SENSIBLE_SORT = (a,b) => a-b;

