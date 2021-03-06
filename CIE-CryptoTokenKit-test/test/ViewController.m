//
//  ViewController.m
//  test
//
//  Created by Dirk-Willem van Gulik on 25-08-14.
//  Copyright (c) 2014 Dirk-Willem van Gulik. All rights reserved.
//

#import "ViewController.h"
#import <CryptoTokenKit/CryptoTokenKit.h>

#define VERBOSE_LOG 1

@interface ViewController ()
@property (nonatomic, retain) TKSmartCardSlotManager * mngr;
@property (nonatomic, retain) NSMutableArray * slots;
@property (nonatomic, retain) NSMutableArray * cards;
@property (nonatomic, retain) IBOutlet NSTextField *status;
@property (nonatomic, retain) IBOutlet NSTextField *NIS;
@end

@implementation ViewController

- (void)viewDidLoad
{
    [super viewDidLoad];
                                    
    self.mngr = [TKSmartCardSlotManager defaultManager];
    assert(self.mngr);
    
    self.status.stringValue = @"";
    self.NIS.stringValue = @"";

    // Observe readers joining and leaving.
    //
    [self.mngr addObserver:self forKeyPath:@"slotNames" options:NSKeyValueObservingOptionNew | NSKeyValueObservingOptionOld | NSKeyValueObservingOptionInitial context:nil];

}

-(void)dealloc {
    [self.mngr removeObserver:self forKeyPath:@"slotNames"];

    for(id slot in self.slots)
        [slot removeObserver:self];
    
    for(id card in self.cards)
        [card removeObserver:self];
}

- (void)observeValueForKeyPath:(NSString *)keyPath
                      ofObject:(id)object
                        change:(NSDictionary *)change
                       context:(void *)context
{
    if([keyPath isEqualToString:@"slotNames"])
    {
        NSLog(@"(Re)Scanning Slots: %@",[self.mngr slotNames]);
        
        // Purge any old observing and rebuild the array.
        //
        for(id slot in _slots)
        {
            [slot removeObserver:self forKeyPath:@"state"];
        }

        for(id card in self.cards)
            [card removeObserver:self forKeyPath:@"valid"];
        
        self.slots = [[NSMutableArray alloc] init];
        self.cards = [[NSMutableArray alloc] init];

        for(NSString *slotName in [_mngr slotNames])
        {
            [_mngr getSlotWithName:slotName reply:^(TKSmartCardSlot *slot) {
                [_slots addObject:slot];
                
                [slot addObserver:self forKeyPath:@"state" options:NSKeyValueObservingOptionNew | NSKeyValueObservingOptionOld | NSKeyValueObservingOptionInitial context:nil];
                
#if VERBOSE_LOG
                NSLog(@"Slot:    %@",slot);
                NSLog(@"  name:  %@",slot.name);
                NSLog(@"  state: %@",[self stateString:slot.state]);
#endif
                self.status.stringValue = [self stateString:slot.state];
            }];
        };
    }  // end of Slot change
    else if ([keyPath isEqualToString:@"state"])
    {
        TKSmartCardSlot * slot = object;
        NSLog(@"  state: %@ for %@",[self stateString:slot.state], slot);
        self.status.stringValue = [self stateString:slot.state];
        self.NIS.stringValue = @"";
        
        if(slot.state == TKSmartCardSlotStateValidCard)
        {
            NSLog(@"  atr:   %@",slot.ATR);
            
            TKSmartCardATRInterfaceGroup * iface = [slot.ATR interfaceGroupForProtocol:TKSmartCardProtocolT1];
            NSLog(@"Iface for T1: %@", iface);
            
            TKSmartCard * sc = [slot makeSmartCard];
            [_cards addObject:sc];
            
            [sc addObserver:self forKeyPath:@"valid" options:NSKeyValueObservingOptionNew | NSKeyValueObservingOptionOld | NSKeyValueObservingOptionInitial context:nil];
            
            NSLog(@"Card: %@", sc);
            NSLog(@"Allowed protocol bitmask: %lx", sc.allowedProtocols);
            
            if (sc.allowedProtocols & TKSmartCardProtocolT0)
                NSLog(@"        T0");
            if (sc.allowedProtocols & TKSmartCardProtocolT1)
                NSLog(@"        T1");
            if (sc.allowedProtocols & TKSmartCardProtocolT15)
                NSLog(@"        T15");
        }

    }
    else if ([keyPath isEqualToString:@"valid"])
    {
        TKSmartCard *sc = object;
        
        if(sc.valid)
        {
            [sc beginSessionWithReply:^(BOOL success, NSError *error) {
                [self sessionStartedWithSmartCard:sc withCompletion:^{
                    [sc endSession];
                }];
            }];
        }
    } // end of TKSmartCard sc valid change
    else
    {
        NSLog(@"Ignored...");
    }
}

- (void)sessionStartedWithSmartCard:(TKSmartCard *)sc withCompletion:(void (^)(void))complete
{
#if VERBOSE_LOG
    NSLog(@"Card in slot <%@>",sc.slot.name);
    NSLog(@"   now in session, selected protocol: %lx", sc.currentProtocol);
#endif
    
    assert(sc.currentProtocol != TKSmartCardProtocolNone);
    
    [self selectIASOnSmartCard:sc withComplete:^(BOOL ok) {
        if(ok)
        {
            NSLog(@">>>>> select IAS ok");
            [self selectCIEOnSmartCard:sc withComplete:^(BOOL ok) {
                if(ok)
                {
                    NSLog(@">>>>> select DF_CIE ok");
                    [self readNISOnSmartCard:sc withComplete:^(BOOL ok, NSString *nis) {
                        if(ok)
                        {
                            NSLog(@">>>>> read NIS ok %@", nis);
                            self.NIS.stringValue = nis;
                        }
                        else
                        {
                            NSLog(@"Errore nella lettura dell'Id_Servizi");
                        }
                        complete();
                    }];
                }
                else
                {
                    NSLog(@"Errore nella selezione del DF_CIE");
                    complete();
                }
            }];
        }
        else
        {
            NSLog(@"Errore nella selezione del DF_IAS");
            complete();
        }
    }];
}

- (void)selectIASOnSmartCard:(TKSmartCard *)sc withComplete:(void (^)(BOOL ok))complete
{
    // prepara la prima APDU: Seleziona il DF dell'applicazione IAS

    unsigned char aidbytes[] = { 0xA0, 0x00, 0x00, 0x00, 0x30, 0x80, 0x00, 0x00, 0x00, 0x09, 0x81, 0x60, 0x01 }; // AID
    sc.cla = 0x00;
    [sc sendIns:0xa4 // select file
             p1:0x04
             p2:0x0c
           data:[NSData dataWithBytes:aidbytes length:sizeof(aidbytes)]
             le:@0
          reply:^(NSData *replyData, UInt16 sw, NSError *error) {
#if VERBOSE_LOG
              NSLog(@"error %@", error);
              NSLog(@"reply %@", replyData);
              NSLog(@"status word %x (expected 0x9000)", sw);
#endif

              if(sw == 0x9000)
                  complete(YES);
              else
                  complete(NO);
          }
     ];
}

- (void)selectCIEOnSmartCard:(TKSmartCard *)sc withComplete:(void (^)(BOOL ok))complete
{
    // prepara la seconda APDU: Seleziona il DF degli oggetti CIE

    unsigned char ciebytes[] = { 0xA0, 0x00, 0x00, 0x00, 0x00, 0x39 }; // CIE
    sc.cla = 0x00;
    [sc sendIns:0xa4 // select file
             p1:0x04
             p2:0x0c
           data:[NSData dataWithBytes:ciebytes length:sizeof(ciebytes)]
             le:@0
          reply:^(NSData *replyData, UInt16 sw, NSError *error) {
#if VERBOSE_LOG
              NSLog(@"error %@", error);
              NSLog(@"reply %@", replyData);
              NSLog(@"status word %x (expected 0x9000)", sw);
#endif

              if(sw == 0x9000)
                  complete(YES);
              else
                  complete(NO);
          }
     ];
}

- (void)readNISOnSmartCard:(TKSmartCard *)sc withComplete:(void (^)(BOOL ok, NSString *nis))complete
{
    // prepara la terza APDU: Lettura del file dell'ID_Servizi selezionato contestualmente tramite Short Identifier (SFI = 1)

    sc.cla = 0x00;
    [sc sendIns:0xb0 // read binary
             p1:0x81 // Read by SFI & SFI = 1
             p2:0x00 // Offset = 0
           data:nil
             le:@(0x0c)
          reply:^(NSData *replyData, UInt16 sw, NSError *error) {
#if VERBOSE_LOG
              NSLog(@"error %@", error);
              NSLog(@"reply %@", replyData);
              NSLog(@"status word %x (expected 0x9000)", sw);
#endif
              if(sw == 0x6a82)
                  NSLog(@"file not found");
              
              if(sw == 0x9000)
              {
                  NSString *nis = [[NSString alloc] initWithData:replyData encoding:NSUTF8StringEncoding];
                  complete(YES, nis);
              }
              else
                  complete(NO, @"");
          }
     ];
}

- (NSString *)stateString:(TKSmartCardSlotState)state
{
    switch (state) {
        case TKSmartCardSlotStateEmpty:
            return @"slot empty";
            break;
        case TKSmartCardSlotStateMissing:
            return @"card missing";
            break;
        case TKSmartCardSlotStateMuteCard:
            return @"mute card";
            break;
        case TKSmartCardSlotStateProbing:
            return @"card probing";
            break;
        case TKSmartCardSlotStateValidCard:
            return @"card valid";
            break;
        default:
            return @"error";
            break;
    }
    return @"bug";
}
@end
