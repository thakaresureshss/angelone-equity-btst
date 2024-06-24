package com.trade.algotrade.service.impl;

import com.trade.algotrade.client.angelone.AngelOneClient;
import com.trade.algotrade.client.kotak.KotakClient;
import com.trade.algotrade.client.kotak.response.ScripResponse;
import com.trade.algotrade.constants.AngelOneConstants;
import com.trade.algotrade.constants.ConfigConstants;
import com.trade.algotrade.constants.Constants;
import com.trade.algotrade.constants.ErrorCodeConstants;
import com.trade.algotrade.enitiy.AngelKotakInstrumentMappingEntity;
import com.trade.algotrade.enitiy.AngelOneInstrumentMasterEntity;
import com.trade.algotrade.enitiy.KotakInstrumentMasterEntity;
import com.trade.algotrade.enitiy.equity.StockMaster;
import com.trade.algotrade.enums.BrokerEnum;
import com.trade.algotrade.exceptions.AlgotradeException;
import com.trade.algotrade.repo.AngelKotakInstrumentMappingRepo;
import com.trade.algotrade.repo.AngelOneInstrumentMasterRepo;
import com.trade.algotrade.repo.InstrumentMasterRepo;
import com.trade.algotrade.response.InstrumentResponse;
import com.trade.algotrade.service.InstrumentService;
import com.trade.algotrade.service.NotificationService;
import com.trade.algotrade.service.UserService;
import com.trade.algotrade.service.equity.StockMasterService;
import com.trade.algotrade.service.mapper.InstrumentMapper;
import com.trade.algotrade.utils.CommonUtils;
import com.trade.algotrade.utils.DateUtils;
import com.trade.algotrade.utils.TradeUtils;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URL;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * @author suresh.thakare
 */

@Service
public class InstrumentServiceImpl implements InstrumentService {

    @Autowired
    KotakClient kotakClient;
    @Autowired
    AngelOneClient angelOneClient;

    @Autowired
    InstrumentMasterRepo instrumentMasterRepo;


    @Autowired
    AngelOneInstrumentMasterRepo angelOneInstrumentMasterRepo;

    @Autowired
    AngelKotakInstrumentMappingRepo angelKotakInstrumentMappingRepo;

    @Autowired
    CommonUtils commonUtils;

    @Autowired
    StockMasterService stockMasterService;

    @Autowired
    TradeUtils tradeUtils;

    @Autowired
    NotificationService notificationService;

    @Autowired
    UserService userService;

    @Autowired
    InstrumentMapper instrumentMapper;

    private static final Logger logger = LoggerFactory.getLogger(InstrumentServiceImpl.class);


    @Override
    public void downlodInstruments() {
        logger.info("Downloading Instrument Started");
//        CompletableFuture<List<AngelOneInstrumentMasterEntity>> angelOneInstrumentCompletable = CompletableFuture.supplyAsync(this::downloadAndProcessAngelInstrument).thenApplyAsync(angelOneInstruments -> {
//            if (!CollectionUtils.isEmpty(angelOneInstruments)) {
//                logger.debug("Save Angel Instruments called");
//                saveAngelOneInstruments(angelOneInstruments);
//                logger.info("Save Angel Instruments Completed");
//            }
//            return angelOneInstruments;
//        });
        List<AngelOneInstrumentMasterEntity> angelOneInstruments = downloadAndProcessAngelInstrument();
        if (!CollectionUtils.isEmpty(angelOneInstruments)) {
            logger.debug("Save Angel Instruments called");
            saveAngelOneInstruments(angelOneInstruments);
            logger.info("Save Angel Instruments Completed");
        }
//        CompletableFuture<List<KotakInstrumentMasterEntity>> kotakInstrumentCompletable = CompletableFuture.supplyAsync(this::downloadAndSaveKotakInstruments).thenApplyAsync(instrumentMasterEntities -> {
//            if (!CollectionUtils.isEmpty(instrumentMasterEntities)) {
//                logger.debug("Save Kotak Instruments Started");
//                deleteOldInstruments();
//                saveInstruments(instrumentMasterEntities);
//                logger.info("Save Kotak Instruments Completed");
//            }
//            return instrumentMasterEntities;
//        });
//        CompletableFuture<Void> combinedFuture = CompletableFuture.allOf(angelOneInstrumentCompletable, kotakInstrumentCompletable);
        logger.debug("Joining angelOneInstrumentCompletable and  kotakInstrumentCompletable");
//        combinedFuture.thenAccept(result -> {
//            try {
//                List<AngelOneInstrumentMasterEntity> angeInstruments = angelOneInstrumentCompletable.get();
//                List<KotakInstrumentMasterEntity> kotakInstrumentMasterEntities = kotakInstrumentCompletable.get();
//                if (!CollectionUtils.isEmpty(angeInstruments) && !CollectionUtils.isEmpty(kotakInstrumentMasterEntities)) {
//                    logger.debug("Kotak-Angel Instruments Mapping Save Started");
//                    saveAngelOneAndKotakInstrumentMapping(kotakInstrumentMasterEntities, angeInstruments);
//                    logger.info("Kotak-Angel Instruments Mapping Save Completed");
//                }
//            } catch (InterruptedException e) {
//                throw new RuntimeException(e);
//            } catch (ExecutionException e) {
//                throw new RuntimeException(e);
//            }
//        });
        logger.info("Downloading Instrument Completed");
        userService.getAllActiveUsersByBroker(BrokerEnum.ANGEL_ONE.toString()).forEach(userResponse -> notificationService.sendTelegramNotification(userResponse.getTelegramChatId(), "Download instrument call successful."));
    }

    private List<KotakInstrumentMasterEntity> downloadFnoInstruments(ScripResponse scripDetails) {
        for (int i = 1; i <= Constants.MAX_RETRY_DOWNLOAD_INSTRUMENT; i++) {
            String fnoFileUrl = scripDetails.getSuccess().getFno();
            try {
                return downloadScripFileData(fnoFileUrl);
            } catch (Exception e) {
                logger.error("******* [InstrumentServiceImpl][downloadAndSaveKotakInstruments] Exception in CASH Instrument Download := {}", e.getMessage());
            }
            logger.info("Retrying FNO Instrument Attempt {} for File {}", i, fnoFileUrl);
        }
        return null;
    }

    private ScripResponse getScripUrls() {
        ScripResponse scripDetails = new ScripResponse();
        for (int i = 1; i <= Constants.MAX_RETRY_DOWNLOAD_INSTRUMENT; i++) {
            try {
                scripDetails = kotakClient.getScripDetails();
            } catch (Exception e) {
                logger.error("******* [InstrumentServiceImpl][downloadAndSaveScripData] Error := {}", e.getMessage());
                try {
                    Thread.sleep(15000);
                } catch (InterruptedException ex) {
                    throw new RuntimeException(ex);
                }
                logger.info("Retrying Attempt {} for Get Scrip URLS", i);
                continue;
            }
            break;
        }
        return scripDetails;
    }

    @Override
    public List<AngelOneInstrumentMasterEntity> downloadAndProcessAngelInstrument() {
        logger.info("Downloading Angel Instrument Started");
        deleteOldAngelOneInstruments();
        //Angel One instrument download and mapping with Kotak instruments.
        List instrumentDetails = angelOneClient.getScripDetails();
        String configValue = commonUtils.getConfigValue(ConfigConstants.INSTRUMENT_DOWNLOAD_BATCH_SIZE);
        int batchSize = StringUtils.isNoneEmpty(configValue) ? Integer.parseInt(configValue) : 500;
        List<List<LinkedHashMap>> downloadedInstruments = TradeUtils.splitIntoBatches(instrumentDetails, batchSize);
        List<CompletableFuture<List<AngelOneInstrumentMasterEntity>>> threads = null;
        logger.info("Processing Angel Instrument in {} Batches", downloadedInstruments.size());
        if (!CollectionUtils.isEmpty(downloadedInstruments)) {
            threads = downloadedInstruments.stream().map(linkedHashMaps -> {
                return CompletableFuture.supplyAsync(() -> {
                    return processAngeloneInstruments(linkedHashMaps);
                });
            }).collect(Collectors.toList());
        }
        List<AngelOneInstrumentMasterEntity> instrumentMasterEntities = threads.stream()
                .flatMap(completableFuture -> completableFuture.join().stream())
                .collect(Collectors.toList());
        logger.info("Downloading Angel Instrument Completed");
        return instrumentMasterEntities;
    }

    @Override
    public List<AngelOneInstrumentMasterEntity> processAngeloneInstruments(List<LinkedHashMap> linkedHashMaps) {
        logger.debug("******* [InstrumentServiceImpl][Angel One - downloadInstrumentData] Called");
        if (!CollectionUtils.isEmpty(linkedHashMaps)) {
            List<AngelOneInstrumentMasterEntity> angelOneInstrumentMasterEntities = new ArrayList<>();
            String currentExpiry = commonUtils.findCurrentExpiryDate();
            linkedHashMaps.stream().forEach(linkedHashMap -> {
                if (Constants.BANK_NIFTY_INDEX.equalsIgnoreCase(linkedHashMap.get("symbol").toString())
                        && Constants.NSE.equalsIgnoreCase(linkedHashMap.get("exch_seg").toString())) {
                    AngelOneInstrumentMasterEntity angelOneInstrumentMasterEntity = AngelOneInstrumentMasterEntity.builder()
                            .instrumentToken(Long.valueOf((String) linkedHashMap.get("token")))
                            .instrumentName((String) linkedHashMap.get("symbol"))
                            .expiry("0")
                            .strike(0)
                            .instrumentType(linkedHashMap.get("instrumenttype").toString())
                            .lotSize(Integer.getInteger(linkedHashMap.get("lotsize").toString()))
                            .optionType("-")
                            .createdTime(DateUtils.getCurrentDateTimeIst())
                            .updatedTime(DateUtils.getCurrentDateTimeIst())
                            .tickSize(new BigDecimal(0)).build();
                    TradeUtils.angelOneInstruments.add(angelOneInstrumentMasterEntity);
                    addNiftyBankIndexInstrument(angelOneInstrumentMasterEntity.getInstrumentToken());
                    angelOneInstrumentMasterEntities.add(angelOneInstrumentMasterEntity);
                }
                if (Constants.BANK_NIFTY.equalsIgnoreCase(linkedHashMap.get("name").toString())
                        && AngelOneConstants.NFO.equalsIgnoreCase(linkedHashMap.get("exch_seg").toString())
                        && linkedHashMap.get("symbol").toString().length() == 23) {
                    String expiry = linkedHashMap.get("symbol").toString().substring(9, 16);

                    if (currentExpiry.equalsIgnoreCase(expiry)) {
                        AngelOneInstrumentMasterEntity angelOneInstrumentMasterEntity = mapJsonRecordToInstrument(linkedHashMap, expiry);
                        TradeUtils.angelOneInstruments.add(angelOneInstrumentMasterEntity);
                        angelOneInstrumentMasterEntities.add(angelOneInstrumentMasterEntity);
                    }
                }
                if (Constants.INDIA_VIX_INDEX.equalsIgnoreCase(linkedHashMap.get("name").toString())) {
                    AngelOneInstrumentMasterEntity angelOneInstrumentMasterEntity = mapIndiaVixJsonRecordToInstrument(linkedHashMap);
                    TradeUtils.angelOneInstruments.add(angelOneInstrumentMasterEntity);
                    angelOneInstrumentMasterEntities.add(angelOneInstrumentMasterEntity);
                }
            });
            return angelOneInstrumentMasterEntities;
        }
        logger.info("******* [InstrumentServiceImpl][Angel One - downloadInstrumentData] Complete");
        return null;
    }

    public List<KotakInstrumentMasterEntity> downloadScripFileData(String fileUrl) {
        logger.info("******* [InstrumentServiceImpl][downloadScripFileData] Called for File URL {}", fileUrl);
        Reader targetReader = null;
        CSVParser csvParser = null;
        List<KotakInstrumentMasterEntity> instrumentMasters = new ArrayList<>();
        try {
            URL url = new URL(fileUrl);
            InputStream is = url.openStream();
            targetReader = new InputStreamReader(is);
            csvParser = new CSVParser(targetReader,
                    CSVFormat.DEFAULT.withFirstRecordAsHeader().withIgnoreHeaderCase().withTrim().withDelimiter('|'));
            int recordCount = 0;
            String currentExpiry = commonUtils.findCurrentExpiryDate();
            for (CSVRecord record : csvParser) {
                String expriy = record.get("expiry");
                if (Constants.BANK_NIFTY.equalsIgnoreCase(record.get("instrumentName"))
                        && Constants.NFO.equalsIgnoreCase(record.get("segment"))
                        && currentExpiry.equalsIgnoreCase(expriy)) {
                    instrumentMasters.add(mapCsvRecordToInstrument(record, expriy));
                }
                recordCount++;
            }
            logger.info("Total Records  Found in CSV File {}", recordCount);

        } catch (IOException e) {
            logger.error("[InstrumentServiceImpl][downloadScripFileData] File URL {} IOException Occurred {}", fileUrl, e.getMessage());
        } catch (Exception e) {
            logger.error("[InstrumentServiceImpl][downloadScripFileData] File URL {}  Exception Occurred {} ", fileUrl, e.getMessage());
        } finally {
            try {
                if (targetReader != null) {
                    targetReader.close();
                }
                if (csvParser != null && !csvParser.isClosed()) {
                    csvParser.close();
                }
            } catch (IOException ex) {
                logger.error(
                        "[InstrumentServiceImpl][downloadScripFileData] Exception Occurred {}", ex.getMessage());
            }
        }
        logger.info("******* [InstrumentServiceImpl][downloadScripFileData] Completed for File URL {}", fileUrl);
        return instrumentMasters;
    }

    private void addCashIntrument(List<KotakInstrumentMasterEntity> instrumentMasters, CSVRecord record, String expriy) {
        KotakInstrumentMasterEntity cashStock = mapCsvRecordToInstrument(record, expriy);
        instrumentMasters.add(cashStock);
    }

    private static void updateStockPrices(StockMaster existingStock, CSVRecord record) {
        BigDecimal ltp = new BigDecimal(record.get("lastPrice"));
        existingStock.setInstrumentToken(Long.valueOf(record.get("instrumentToken")));
        BigDecimal yesterDayClose = existingStock.getPreviousClose() != null
                ? existingStock.getPreviousClose()
                : ltp;
        existingStock.setChangePercent(TradeUtils.getPercent(yesterDayClose, ltp));
        existingStock.setLtp(ltp);
        existingStock.setLastUpdatedAt(DateUtils.getCurrentDateTimeIst());
    }

    private void addNiftyBankIndexInstrument(Long instrument) {
        try {
            tradeUtils.addWebsocketInstruments(instrument, Constants.BANK_NIFTY_INDEX);
        } catch (DuplicateKeyException duplicateKeyException) {
            logger.error("Duplicate Instrument Found : Ignoring error");
        }
    }

    private KotakInstrumentMasterEntity mapCsvRecordToInstrument(CSVRecord record, String expriy) {
        KotakInstrumentMasterEntity scripMaster = KotakInstrumentMasterEntity.builder()
                .instrumentToken(Long.valueOf(record.get("instrumentToken"))).exchange(record.get("exchange"))
                .expiry(expriy).instrumentName(record.get("instrumentName"))
                .lotSize(Integer.valueOf(record.get("lotSize"))).tickSize(new BigDecimal(record.get("tickSize")))
                .segment(record.get("segment")).strike(Integer.valueOf(record.get("strike")))
                .createdTime(DateUtils.getCurrentDateTimeIst()).optionType(record.get("optionType")).updatedTime(DateUtils.getCurrentDateTimeIst())
                .lastPrice(new BigDecimal(record.get("lastPrice"))).build();
        scripMaster.setTickSize(new BigDecimal(record.get("tickSize")));
        return scripMaster;
    }

    private AngelOneInstrumentMasterEntity mapJsonRecordToInstrument(LinkedHashMap linkedHashMap, String expiry) {
        return AngelOneInstrumentMasterEntity.builder()
                .instrumentToken(Long.valueOf((String) linkedHashMap.get("token")))
                .instrumentName((String) linkedHashMap.get("name"))
                .expiry(expiry)
                .strike(Integer.valueOf(linkedHashMap.get("symbol").toString().substring(16, 21)))
                .instrumentType(linkedHashMap.get("instrumenttype").toString())
                .lotSize(Integer.valueOf(linkedHashMap.get("lotsize").toString()))
                .optionType(linkedHashMap.get("symbol").toString().substring(21, 23))
                .tickSize(new BigDecimal(linkedHashMap.get("tick_size").toString()))
                .tradingSymbol(linkedHashMap.get("symbol").toString())
                .build();
    }

    private AngelOneInstrumentMasterEntity mapIndiaVixJsonRecordToInstrument(LinkedHashMap linkedHashMap) {
        return AngelOneInstrumentMasterEntity.builder()
                .instrumentToken(Long.valueOf((String) linkedHashMap.get("token")))
                .instrumentName((String) linkedHashMap.get("name"))
                .build();
    }


    public void saveAngelOneAndKotakInstrumentMapping(List<KotakInstrumentMasterEntity> kotakInstruments, List<AngelOneInstrumentMasterEntity> angelOneInstruments) {
        try {
            angelKotakInstrumentMappingRepo.deleteAll();
            kotakInstruments.forEach(kotakInstrument -> {
                angelOneInstruments.forEach(angelOneInstrument -> {
                    if (kotakInstrument.getSegment().equalsIgnoreCase("FO") || kotakInstrument.getInstrumentName().equalsIgnoreCase(Constants.BANK_NIFTY_INDEX)) {
                        if (Objects.equals(kotakInstrument.getStrike(), angelOneInstrument.getStrike())
                                && kotakInstrument.getOptionType().equalsIgnoreCase(angelOneInstrument.getOptionType())
                                && kotakInstrument.getExpiry().equals(angelOneInstrument.getExpiry())) {
                            //keeping two way mapping for easy search
                            AngelKotakInstrumentMappingEntity mappingEntity = AngelKotakInstrumentMappingEntity.builder()
                                    .angelInstrument(angelOneInstrument.getInstrumentToken())
                                    .kotakInstrument(kotakInstrument.getInstrumentToken())
                                    .createdTime(DateUtils.getCurrentDateTimeIst())
                                    .updatedTime(DateUtils.getCurrentDateTimeIst())
                                    .build();
                            angelKotakInstrumentMappingRepo.save(mappingEntity);
                        }
                    }
                });
            });
            logger.debug("***** Kotak and Angel one instruments mapping completed");
        } catch (Exception e) {
            logger.error("Exception Occurred while Saving AngelOne and Kotak Instrument Mapping to DB {}", e.getMessage());
        }
    }

    @Override
    public List<AngelKotakInstrumentMappingEntity> getAngelKotakMapping() {
        return angelKotakInstrumentMappingRepo.findAll();
    }

    @Override
    @Cacheable(value = "angelKotakInstrumentMapping", key = "#instrumentToken")
    public Long getKotakInstrumentByAngel(Long instrumentToken) {
        logger.info("Finding Kotak Instrument By Providing Angel Instrument {}", instrumentToken);
        return angelKotakInstrumentMappingRepo.findByAngelInstrument(instrumentToken).orElseThrow(() -> new AlgotradeException(ErrorCodeConstants.KOTAK_ANGEL_INSTRUMENT_MAPPING_NOT_FOUND)).getKotakInstrument();
    }

    @Override
    @Cacheable(value = "kotakAngelInstrumentMapping", key = "#instrumentToken")
    public Long getAngelInstrumentByKotak(Long instrumentToken) {
        return angelKotakInstrumentMappingRepo.findByKotakInstrument(instrumentToken).orElseThrow(() -> new AlgotradeException(ErrorCodeConstants.KOTAK_ANGEL_INSTRUMENT_MAPPING_NOT_FOUND)).getAngelInstrument();
    }

    @Override
    public Optional<AngelOneInstrumentMasterEntity> getAngelOneInstrument(String instrumentName) {
        return angelOneInstrumentMasterRepo.findByInstrumentNameIgnoreCase(instrumentName);
    }

    @Override
    public List<AngelOneInstrumentMasterEntity> getAllAngelInstrumentsByStrike(List<Long> strikelist) {
        return angelOneInstrumentMasterRepo.findByStrikeIn(strikelist);
    }

    @Override
    public InstrumentResponse getInstrumentByTokenTest(Long instrumentToken) {
        AngelOneInstrumentMasterEntity entity = getInstrumentByToken(instrumentToken);
        return instrumentMapper.entityToInstrumentResponse(entity);
    }

    @Override
    @Cacheable("instrumentMasters")
    public List<AngelOneInstrumentMasterEntity> getAllInstruments() {
        logger.debug("******* [InstrumentServiceImpl][getAllInstruments] Called");
        return instrumentMasterRepo.findAll();
    }


    @Override
    public AngelOneInstrumentMasterEntity findByInstrumentName(String bankNiftyIndex) {
        Optional<AngelOneInstrumentMasterEntity> optionalInstrumentName = instrumentMasterRepo
                .findByInstrumentNameIgnoreCase(bankNiftyIndex);
        return optionalInstrumentName.orElse(null);
    }

    @Override
    @Cacheable(value = "instrumentMasters", key = "#instrumentToken")
    public AngelOneInstrumentMasterEntity getInstrumentByToken(Long instrumentToken) {
        return instrumentMasterRepo.findByInstrumentToken(instrumentToken);
    }


    @Override
    @CacheEvict(cacheNames = "angelOneInstrumentMasters", allEntries = true)
    public void deleteOldAngelOneInstruments() {
        angelOneInstrumentMasterRepo.deleteAll();
    }

    @Override
    @CacheEvict(value = {"angelOneInstrumentMasters", "instrumentMasters"}, allEntries = true)
    public void saveAngelOneInstruments(List<AngelOneInstrumentMasterEntity> instrumentMasters) {
        try {
//            angelOneInstrumentMasterRepo.saveAll(instrumentMasters);
            instrumentMasterRepo.saveAll(instrumentMasters);
        } catch (Exception e) {
            logger.error("Exception Occurred while saving Instruments to DB {}", e.getMessage());
        }
    }

}