extern crate bytes;

use bytes::BufMut;
use std::net::IpAddr;

pub const VERSION: u8 = 2;
pub const PORT_CLIENT: u16 = 5350;
pub const PORT_SERVER: u16 = 5351;

pub trait ToBytes {
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

impl ToBytes for Response {

    fn length(&self) -> usize {
        32 * 6 // TODO: Support Opcode data and options
    }

    fn to_bytes(&self) -> Vec<u8> {
        let mut bytes = Vec::with_capacity(self.length());
        bytes.put_u8(self.version);
        bytes.put_u8(0b10000000); // TODO: Support real Opcode
        bytes.put_u8(0);
        bytes.put_u8(self.result_code);
        bytes.put_u32_be(self.lifetime);
        bytes.put_u32_be(self.epoch);
        bytes.put_slice(&[0; 96 / 8]);
        // TODO: Support Opcode data and options

        bytes
    }
}
