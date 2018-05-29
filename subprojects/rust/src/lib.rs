use std::net::IpAddr;

pub const VERSION: u8 = 2;

pub trait Datagram : Packet {
    fn lifetime(&self) -> &u32;
    fn opcode(&self) -> Opcode;
    fn version(&self) -> &u8;
}

pub trait Packet {
    fn size(&self) -> usize;
    fn to_bytes(&self) -> Vec<u8>;
}

pub trait OperationData: Packet {
    fn opcode(&self) -> Opcode;
}

pub enum Opcode {
    Announce,
    Map,
    Peer
}

pub struct Request {
    pub lifetime: u32,
    pub operation_data: Box<OperationData + Send + Sync>,
    pub version: u8
}

impl Request {
    pub fn from_bytes(data: &[u8]) -> Result<Self, String> {
        Result::Err(String::from("Unsupported operation")) // TODO
    }
}

impl Datagram for Request {
    fn lifetime(&self) -> &u32 {
        &self.lifetime
    }
    fn opcode(&self) -> Opcode {
        self.operation_data.opcode()
    }
    fn version(&self) -> &u8 {
        &self.version
    }
}

impl Packet for Request {
    fn size(&self) -> usize {
        0 // TODO
    }
    fn to_bytes(&self) -> Vec<u8> {
        vec!(0) // TODO
    }
}

pub struct MapData {
    pub ip_external: IpAddr,
    pub ip_internal: IpAddr,
    pub port_external: u16,
    pub port_internal: u16,
    pub protocol: u8,
    pub nounce: [u8; 12]
}

impl OperationData for MapData {
    fn opcode(&self) -> Opcode {
        Opcode::Map
    }
}

impl Packet for MapData {
    fn size(&self) -> usize {
        36
    }
    fn to_bytes(&self) -> Vec<u8> {
        vec!(0) // TODO
    }
}

pub struct Response {
    pub epoch: u32,
    pub lifetime: u32,
    pub operation_data: Box<OperationData + Send + Sync>,
    pub result: u8,
    pub version: u8
}
