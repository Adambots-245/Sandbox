// A small NetworkTables 4 client for the browser.
// Protocol: ntcore/doc/networktables4.adoc in wpilibsuite/allwpilib.
//
// How NT4 works, in short:
//  - The robot runs the server on port 5810. We open a WebSocket to ws://<host>:5810/nt/<name>.
//  - Text frames are JSON arrays of {method, params} messages: we "subscribe" to topics,
//    the server "announce"s each topic (its id, name and type).
//  - Binary frames carry values, MessagePack-encoded: [topicId, timestampMicros, typeId, value].
//  - To set a value we "publish" a topic with our own id (pubuid), then send [pubuid, time, typeId, value].
//    The server keeps the newest value by time, so we first ask for the server's clock (time sync).

import { decodeAll, encode } from "./msgpack.js";

const PROTOCOLS = ["v4.1.networktables.first.wpi.edu", "networktables.first.wpi.edu"];
const PORT = 5810;
const RECONNECT_MS = 1000;

// NT4 type names and their binary type ids.
const TYPE_IDS = { boolean: 0, double: 1, int: 2, float: 3, string: 4, raw: 5 };

export class NT4Client {
  /**
   * @param {string} prefix topics to subscribe to, for example "/Visualizer/"
   * @param {(status: {connected: boolean, url: string}) => void} onStatus called when the connection changes
   */
  constructor(prefix, onStatus) {
    this.prefix = prefix;
    this.onStatus = onStatus;
    this.host = "localhost";
    this.socket = null;
    this.connected = false;
    this.topicsById = new Map(); // server topic id -> {name, type}
    this.values = new Map(); // topic name -> latest value
    this.publishers = new Map(); // topic name -> {pubuid, type}
    this.nextPubuid = 1;
    this.reconnectTimer = null;
    this.serverTimeOffset = null; // server microseconds minus our microseconds
  }

  get url() {
    return "ws://" + this.host + ":" + PORT + "/nt/visualizer";
  }

  /** Connects to a robot (or the simulator) and keeps reconnecting every second while disconnected. */
  connect(host) {
    this.host = host || "localhost";
    if (this.socket) {
      const old = this.socket;
      this.socket = null;
      old.close();
    }
    this.open();
  }

  open() {
    clearTimeout(this.reconnectTimer);
    this.setStatus(false);
    let socket;
    try {
      socket = new WebSocket(this.url, PROTOCOLS);
    } catch (e) {
      this.scheduleReconnect();
      return;
    }
    socket.binaryType = "arraybuffer";
    this.socket = socket;

    socket.onopen = () => {
      if (socket !== this.socket) return;
      this.topicsById.clear();
      this.values.clear();
      // Subscribe to everything under the prefix, and re-create our publishers.
      const messages = [{
        method: "subscribe",
        params: { topics: [this.prefix], subuid: 1, options: { prefix: true } },
      }];
      for (const [name, pub] of this.publishers) {
        messages.push(publishMessage(name, pub));
      }
      socket.send(JSON.stringify(messages));
      // Time sync: send our clock as topic -1; the server replies with its clock and our time back.
      this.serverTimeOffset = null;
      socket.send(encode([-1, 0, TYPE_IDS.int, nowMicros()]));
      this.setStatus(true);
    };

    socket.onmessage = (event) => {
      if (socket !== this.socket) return;
      if (typeof event.data === "string") this.handleText(event.data);
      else this.handleBinary(event.data);
    };

    socket.onclose = () => {
      if (socket !== this.socket) return; // replaced by a newer connection
      this.socket = null;
      this.setStatus(false);
      this.scheduleReconnect();
    };
    socket.onerror = () => socket.close();
  }

  scheduleReconnect() {
    clearTimeout(this.reconnectTimer);
    this.reconnectTimer = setTimeout(() => this.open(), RECONNECT_MS);
  }

  setStatus(connected) {
    this.connected = connected;
    if (this.onStatus) this.onStatus({ connected, url: this.url });
  }

  handleText(text) {
    for (const message of JSON.parse(text)) {
      const p = message.params;
      if (message.method === "announce") {
        this.topicsById.set(p.id, { name: p.name, type: p.type });
      } else if (message.method === "unannounce") {
        const topic = this.topicsById.get(p.id);
        if (topic) this.values.delete(topic.name);
        this.topicsById.delete(p.id);
      }
      // "properties" messages are not needed here.
    }
  }

  handleBinary(buffer) {
    for (const message of decodeAll(buffer)) {
      if (!Array.isArray(message) || message.length !== 4) continue;
      const [id, serverTime, , value] = message;
      if (id === -1) {
        // Time sync reply: value is the time we sent. Assume the reply took half the round trip.
        const now = nowMicros();
        this.serverTimeOffset = serverTime + (now - value) / 2 - now;
        continue;
      }
      const topic = this.topicsById.get(id);
      if (topic) this.values.set(topic.name, value);
    }
  }

  /** The latest value of a topic, or the fallback if we haven't received one. */
  get(name, fallback) {
    return this.values.has(name) ? this.values.get(name) : fallback;
  }

  /** Sets a boolean topic on the robot. */
  setBoolean(name, value) {
    this.setValue(name, "boolean", value);
  }

  setValue(name, type, value) {
    let pub = this.publishers.get(name);
    if (!pub) {
      pub = { pubuid: this.nextPubuid++, type };
      this.publishers.set(name, pub);
      if (this.connected) this.socket.send(JSON.stringify([publishMessage(name, pub)]));
    }
    if (this.connected) {
      const time = this.serverTimeOffset === null ? 0 : Math.round(nowMicros() + this.serverTimeOffset);
      this.socket.send(encode([pub.pubuid, Math.max(time, 0), TYPE_IDS[type], value]));
    }
  }
}

function nowMicros() {
  return Math.round(performance.now() * 1000);
}

function publishMessage(name, pub) {
  return { method: "publish", params: { name, pubuid: pub.pubuid, type: pub.type, properties: {} } };
}
