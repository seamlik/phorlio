use std::net::IpAddr;

pub const VERSION: u8 = 2;

trait ToBytes {
    fn length(&self) -> usize;
    fn to_bytes(&self) -> Vec<u8>;
}

pub enum Opcode {
    Announce,
    Authentication,
    Map,
    Peer
}

pub struct Request {
    pub client_ip_address: IpAddr,
    pub lifetime: u32,
    pub version: u8
}

impl Request {
    pub fn from_bytes(data: &[u8]) -> Result<Self, String> {
        Result::Err(String::from("Unsupported operation")) // TODO
    }
}

impl ToBytes for Request {
    fn length(&self) -> usize {
        0 // TODO
    }
    fn to_bytes(&self) -> Vec<u8> {
        vec!(0) // TODO
    }
}

pub struct Response {
    pub epoch: u32,
    pub lifetime: u32,
    pub result_code: u8,
    pub version: u8
}
