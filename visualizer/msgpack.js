// A minimal MessagePack decoder and encoder: just enough for NetworkTables 4 value messages.
// Spec: https://github.com/msgpack/msgpack/blob/master/spec.md

const textDecoder = new TextDecoder();
const textEncoder = new TextEncoder();

/** Decodes every MessagePack value in a buffer (NT4 can pack several messages in one frame). */
export function decodeAll(arrayBuffer) {
  const view = new DataView(arrayBuffer);
  const bytes = new Uint8Array(arrayBuffer);
  let pos = 0;
  const values = [];

  function str(length) {
    const s = textDecoder.decode(bytes.subarray(pos, pos + length));
    pos += length;
    return s;
  }
  function bin(length) {
    const b = bytes.slice(pos, pos + length);
    pos += length;
    return b;
  }
  function array(length) {
    const a = [];
    for (let i = 0; i < length; i++) a.push(read());
    return a;
  }
  function map(length) {
    const m = {};
    for (let i = 0; i < length; i++) {
      const key = read();
      m[key] = read();
    }
    return m;
  }

  function read() {
    const type = view.getUint8(pos++);
    if (type <= 0x7f) return type; // positive fixint
    if (type >= 0xe0) return type - 0x100; // negative fixint
    if ((type & 0xf0) === 0x80) return map(type & 0x0f);
    if ((type & 0xf0) === 0x90) return array(type & 0x0f);
    if ((type & 0xe0) === 0xa0) return str(type & 0x1f);
    let v;
    switch (type) {
      case 0xc0: return null;
      case 0xc2: return false;
      case 0xc3: return true;
      case 0xc4: v = view.getUint8(pos); pos += 1; return bin(v);
      case 0xc5: v = view.getUint16(pos); pos += 2; return bin(v);
      case 0xc6: v = view.getUint32(pos); pos += 4; return bin(v);
      case 0xca: v = view.getFloat32(pos); pos += 4; return v;
      case 0xcb: v = view.getFloat64(pos); pos += 8; return v;
      case 0xcc: v = view.getUint8(pos); pos += 1; return v;
      case 0xcd: v = view.getUint16(pos); pos += 2; return v;
      case 0xce: v = view.getUint32(pos); pos += 4; return v;
      case 0xcf: v = Number(view.getBigUint64(pos)); pos += 8; return v;
      case 0xd0: v = view.getInt8(pos); pos += 1; return v;
      case 0xd1: v = view.getInt16(pos); pos += 2; return v;
      case 0xd2: v = view.getInt32(pos); pos += 4; return v;
      case 0xd3: v = Number(view.getBigInt64(pos)); pos += 8; return v;
      case 0xd9: v = view.getUint8(pos); pos += 1; return str(v);
      case 0xda: v = view.getUint16(pos); pos += 2; return str(v);
      case 0xdb: v = view.getUint32(pos); pos += 4; return str(v);
      case 0xdc: v = view.getUint16(pos); pos += 2; return array(v);
      case 0xdd: v = view.getUint32(pos); pos += 4; return array(v);
      case 0xde: v = view.getUint16(pos); pos += 2; return map(v);
      case 0xdf: v = view.getUint32(pos); pos += 4; return map(v);
      default: throw new Error("MessagePack type 0x" + type.toString(16) + " not supported");
    }
  }

  while (pos < bytes.length) values.push(read());
  return values;
}

/**
 * Encodes a value. Supports what the page sends: arrays, booleans, null, strings,
 * whole numbers (as integers) and other numbers (as 64-bit floats).
 */
export function encode(value) {
  const out = [];
  function u8(b) { out.push(b & 0xff); }
  function u16(n) { u8(n >> 8); u8(n); }
  function u32(n) { u16(n >>> 16); u16(n & 0xffff); }

  function write(v) {
    if (v === null || v === undefined) u8(0xc0);
    else if (v === false) u8(0xc2);
    else if (v === true) u8(0xc3);
    else if (typeof v === "number" && Number.isInteger(v) && v >= 0 && v <= 0xffffffff) {
      if (v <= 0x7f) u8(v);
      else if (v <= 0xff) { u8(0xcc); u8(v); }
      else if (v <= 0xffff) { u8(0xcd); u16(v); }
      else { u8(0xce); u32(v); }
    } else if (typeof v === "number" && Number.isSafeInteger(v) && v > 0) {
      const b = new DataView(new ArrayBuffer(8));
      b.setBigUint64(0, BigInt(v));
      u8(0xcf);
      for (let i = 0; i < 8; i++) u8(b.getUint8(i));
    } else if (typeof v === "number" && Number.isInteger(v) && v < 0 && v >= -0x80000000) {
      if (v >= -32) u8(v);
      else { u8(0xd2); u32(v >>> 0); }
    } else if (typeof v === "number") {
      const b = new DataView(new ArrayBuffer(8));
      b.setFloat64(0, v);
      u8(0xcb);
      for (let i = 0; i < 8; i++) u8(b.getUint8(i));
    } else if (typeof v === "string") {
      const s = textEncoder.encode(v);
      if (s.length <= 31) u8(0xa0 | s.length);
      else { u8(0xdb); u32(s.length); }
      for (const b of s) u8(b);
    } else if (Array.isArray(v)) {
      if (v.length <= 15) u8(0x90 | v.length);
      else { u8(0xdc); u16(v.length); }
      for (const item of v) write(item);
    } else {
      throw new Error("Can't encode " + typeof v);
    }
  }

  write(value);
  return new Uint8Array(out);
}
